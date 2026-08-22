package com.zakir.vestra.shared.packs

import com.zakir.vestra.shared.domain.DeviceSpec
import com.zakir.vestra.shared.domain.ModelPack
import com.zakir.vestra.shared.domain.PackVerifyStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import com.zakir.vestra.shared.domain.PackManifest
import com.zakir.vestra.shared.domain.PackState
import com.zakir.vestra.shared.domain.PackStatus

/**
 * Tracks which model packs exist, which are installed, and where their files
 * live. Downloading itself is platform work (WorkManager on Android) driven
 * through [markDownloading]/[completeInstall]; this class owns all state and
 * validation so that logic is shared and unit-testable.
 *
 * Layout: <packsRoot>/<packId>/<version>/<files…> with a `.complete` marker
 * written only after every file passed its sha256 check **and** optional ONNX
 * verification via [integrityChecker].
 */
class ModelPackManager(
    private val fs: PackFileSystem,
    private val device: DeviceProbe,
    private val http: HttpClient,
    private val manifestUrl: String,
    private val integrityChecker: PackIntegrityChecker = NoOpPackIntegrityChecker,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _states = MutableStateFlow<Map<String, PackState>>(emptyMap())
    val states: StateFlow<Map<String, PackState>> = _states

    /** Last catalog-load failure (null when the catalog loaded). Surfaced in the UI. */
    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError

    /** Serializes ONNX verification (install + startup) so parallel callers cannot OOM. */
    private val integrityLock = Any()

    /** Pack IDs referenced by an active engine run — blocks [uninstall]. */
    private val activePackIds = mutableSetOf<String>()

    fun markPackInUse(id: String) {
        synchronized(activePackIds) { activePackIds.add(id) }
    }

    fun markPackIdle(id: String) {
        synchronized(activePackIds) { activePackIds.remove(id) }
    }

    fun isPackInUse(id: String): Boolean = synchronized(activePackIds) { id in activePackIds }

    /** Loads the last manifest fetched (offline start), then refreshes over the network. */
    suspend fun refresh(networkAllowed: Boolean = true) {
        withContext(Dispatchers.Default) {
            // Runs even without a cached catalog so bundled packs are still discovered.
            rebuildStates(cachedManifest() ?: PackManifest(schemaVersion = 1, packs = emptyList()))
        }
        if (!networkAllowed) return
        runCatching {
            val response = http.get(manifestUrl)
            check(response.status.isSuccess()) { "server returned HTTP ${response.status.value}" }
            val body = response.bodyAsText()
            val manifest = json.decodeFromString<PackManifest>(body)
            withContext(Dispatchers.Default) {
                fs.mkdirs(fs.packsRoot())
                fs.writeText(cachePath(), body)
                rebuildStates(manifest)
            }
        }.onSuccess {
            _lastError.value = null
        }.onFailure { e ->
            // Don't hide it: keep any cached catalog on screen, but report why the
            // refresh failed so the UI can show it and offer a retry.
            _lastError.value = e.message ?: e::class.simpleName ?: "unknown error"
        }
    }

    fun pack(id: String): ModelPack? = _states.value[id]?.pack

    /** Directory containing an installed pack's files, or null when not installed. */
    fun installedDir(id: String): String? {
        val pack = pack(id) ?: return null
        val dir = versionDir(pack)
        return dir.takeIf { fs.exists("$dir/$COMPLETE_MARKER") }
    }

    fun isInstalled(id: String): Boolean = installedDir(id) != null

    /** Installed and passed file + ONNX verification — safe for local inference. */
    fun isReady(id: String): Boolean = _states.value[id]?.isReady() == true

    fun verifyStatus(id: String): PackVerifyStatus =
        _states.value[id]?.verifyStatus ?: PackVerifyStatus.UNKNOWN

    /**
     * Re-validates an installed pack (files + ONNX). Updates [states] with
     * [PackVerifyStatus.VERIFIED] or [PackVerifyStatus.FAILED].
     */
    suspend fun verifyInstalled(id: String): Boolean = withContext(Dispatchers.Default) {
        synchronized(integrityLock) {
            val state = _states.value[id] ?: return@withContext false
            if (state.status != PackStatus.INSTALLED) return@withContext false
            val dir = installedDir(id) ?: return@withContext false
            markVerifying(id)
            val error = runIntegrityChecks(state.pack, dir)
            if (error == null) {
                markVerified(id)
                true
            } else {
                markVerifyFailed(id, error)
                false
            }
        }
    }

    /** Verifies every installed pack; call after [refresh] on startup. */
    suspend fun verifyAllInstalled() = withContext(Dispatchers.Default) {
        synchronized(integrityLock) {
            _states.value.values
                .filter { it.status == PackStatus.INSTALLED }
                .forEach { state ->
                    val dir = installedDir(state.pack.id) ?: return@forEach
                    markVerifying(state.pack.id)
                    val error = runIntegrityChecks(state.pack, dir)
                    if (error == null) {
                        markVerified(state.pack.id)
                    } else {
                        markVerifyFailed(state.pack.id, error)
                    }
                }
        }
    }

    fun markDownloading(id: String, progress: Float) {
        updateStatus(id) { it.copy(status = PackStatus.DOWNLOADING, progress = progress) }
    }

    fun markFailed(id: String) {
        updateStatus(id) {
            it.copy(
                status = PackStatus.NOT_INSTALLED,
                progress = 0f,
                verifyStatus = PackVerifyStatus.UNKNOWN,
                verifyError = null,
                verifiedAtMs = null,
            )
        }
    }

    /** Drop an in-flight download UI state while keeping staged bytes for a later resume. */
    fun markCancelled(id: String) {
        updateStatus(id) { current ->
            val staged = stagedBytes(current.pack)
            val progress = (staged.toFloat() / current.pack.totalBytes.coerceAtLeast(1)).coerceIn(0f, 0.99f)
            current.copy(status = PackStatus.NOT_INSTALLED, progress = progress)
        }
    }

    /** Bytes already staged for [pack] (used to restore DOWNLOADING after catalog refresh). */
    fun stagedBytes(pack: ModelPack): Long {
        val staging = stagingDir(pack)
        return pack.files.sumOf { file ->
            val path = "$staging/${file.path}"
            if (fs.exists(path)) fs.fileSize(path).coerceAtMost(file.bytes) else 0L
        }
    }

    fun hasPartialStaging(pack: ModelPack): Boolean = stagedBytes(pack) > 0L

    /**
     * Called by the platform downloader once all files are staged. Verifies
     * every sha256 and ONNX loadability before committing; a corrupt file aborts the install.
     */
    fun completeInstall(id: String, stagingDir: String): Boolean {
        val pack = pack(id) ?: return false
        for (file in pack.files) {
            val staged = "$stagingDir/${file.path}"
            if (!fs.exists(staged) || fs.sha256(staged) != file.sha256) {
                markFailed(id)
                return false
            }
        }
        synchronized(integrityLock) {
            runIntegrityChecks(pack, stagingDir)?.let {
                markFailed(id)
                return false
            }
        }
        val target = versionDir(pack)
        fs.delete(target)
        fs.mkdirs(parentOf(target))
        fs.move(stagingDir, target)
        fs.writeText("$target/$COMPLETE_MARKER", pack.version.toString())
        // Older versions of this pack are dead weight now.
        fs.listFiles("${fs.packsRoot()}/${pack.id}")
            .filter { it != target }
            .forEach(fs::delete)
        updateStatus(id) {
            it.copy(
                status = PackStatus.INSTALLED,
                progress = 1f,
                verifyStatus = PackVerifyStatus.VERIFIED,
                verifyError = null,
                verifiedAtMs = System.currentTimeMillis(),
            )
        }
        return true
    }

    /**
     * Commits a pre-verified directory (e.g. debug bootstrap) without re-checking sha256.
     * Still runs file + ONNX verification when a checker is configured.
     */
    fun commitVerifiedInstall(id: String, sourceDir: String): Boolean {
        val pack = pack(id) ?: return false
        synchronized(integrityLock) {
            runIntegrityChecks(pack, sourceDir)?.let { return false }
        }
        val target = versionDir(pack)
        if (target != sourceDir) {
            fs.delete(target)
            fs.mkdirs(parentOf(target))
            if (fs.exists(sourceDir)) {
                fs.move(sourceDir, target)
            }
        }
        fs.writeText("$target/$COMPLETE_MARKER", pack.version.toString())
        updateStatus(id) {
            it.copy(
                status = PackStatus.INSTALLED,
                progress = 1f,
                verifyStatus = PackVerifyStatus.VERIFIED,
                verifyError = null,
                verifiedAtMs = System.currentTimeMillis(),
            )
        }
        return true
    }

    fun uninstall(id: String): Boolean {
        if (isPackInUse(id)) return false
        fs.delete("${fs.packsRoot()}/$id")
        updateStatus(id) {
            it.copy(
                status = PackStatus.NOT_INSTALLED,
                progress = 0f,
                verifyStatus = PackVerifyStatus.UNKNOWN,
                verifyError = null,
                verifiedAtMs = null,
            )
        }
        return true
    }

    fun deviceMeets(spec: DeviceSpec): Boolean =
        device.totalRamMb() >= spec.minRamMb && (!spec.requiresNpu || device.hasNpu())

    /** True when the volume has room for the pack plus a safety margin. */
    fun hasSpaceFor(pack: ModelPack): Boolean =
        fs.freeBytes() > pack.totalBytes + SPACE_MARGIN_BYTES

    fun stagingDir(pack: ModelPack): String = "${fs.packsRoot()}/.staging/${pack.id}"

    private fun runIntegrityChecks(pack: ModelPack, dir: String): String? {
        integrityChecker.verifyFiles(pack, dir)?.let { return it }
        return integrityChecker.verifyOnnx(pack, dir)
    }

    private fun markVerifying(id: String) {
        updateStatus(id) { it.copy(verifyStatus = PackVerifyStatus.VERIFYING, verifyError = null) }
    }

    private fun markVerified(id: String) {
        updateStatus(id) {
            it.copy(
                verifyStatus = PackVerifyStatus.VERIFIED,
                verifyError = null,
                verifiedAtMs = System.currentTimeMillis(),
            )
        }
    }

    private fun markVerifyFailed(id: String, error: String) {
        updateStatus(id) {
            it.copy(
                verifyStatus = PackVerifyStatus.FAILED,
                verifyError = error,
                verifiedAtMs = null,
            )
        }
    }

    private fun rebuildStates(manifest: PackManifest) {
        val previous = _states.value
        _states.value = withBundledPacks(manifest.packs).associate { pack ->
            val dir = versionDir(pack)
            val markerPath = "$dir/$COMPLETE_MARKER"
            val prior = previous[pack.id]
            val staged = stagedBytes(pack)

            // Drop stale .complete markers when files are missing or wrong size.
            var status = when {
                fs.exists(markerPath) && !filesIntact(pack, dir) -> {
                    fs.delete(markerPath)
                    PackStatus.NOT_INSTALLED
                }
                fs.exists(markerPath) -> PackStatus.INSTALLED
                prior?.status == PackStatus.DOWNLOADING -> PackStatus.DOWNLOADING
                fs.listFiles("${fs.packsRoot()}/${pack.id}")
                    .any { fs.exists("$it/$COMPLETE_MARKER") } -> PackStatus.UPDATE_AVAILABLE
                !deviceMeets(pack.minSpec) -> PackStatus.INCOMPATIBLE
                else -> PackStatus.NOT_INSTALLED
            }
            val verifyStatus = when {
                status != PackStatus.INSTALLED -> PackVerifyStatus.UNKNOWN
                prior?.pack?.version == pack.version &&
                    prior.verifyStatus == PackVerifyStatus.VERIFIED -> PackVerifyStatus.VERIFIED
                else -> PackVerifyStatus.UNKNOWN
            }
            val progress = when (status) {
                PackStatus.DOWNLOADING -> {
                    if (prior?.status == PackStatus.DOWNLOADING && prior.progress > 0f) {
                        prior.progress
                    } else {
                        (staged.toFloat() / pack.totalBytes.coerceAtLeast(1)).coerceIn(0f, 0.99f)
                    }
                }
                PackStatus.INSTALLED -> 1f
                PackStatus.NOT_INSTALLED ->
                    (staged.toFloat() / pack.totalBytes.coerceAtLeast(1)).coerceIn(0f, 0.99f)
                else -> 0f
            }
            pack.id to PackState(
                pack = pack,
                status = status,
                progress = progress,
                verifyStatus = verifyStatus,
                verifyError = prior?.verifyError.takeIf { verifyStatus == PackVerifyStatus.FAILED },
                verifiedAtMs = prior?.verifiedAtMs.takeIf { verifyStatus == PackVerifyStatus.VERIFIED },
            )
        }
    }

    private fun filesIntact(pack: ModelPack, dir: String): Boolean =
        pack.files.all { file ->
            val path = "$dir/${file.path}"
            fs.exists(path) && fs.fileSize(path) == file.bytes
        }

    private fun cachedManifest(): PackManifest? =
        fs.readText(cachePath())?.let { cached ->
            runCatching { json.decodeFromString<PackManifest>(cached) }.getOrNull()
        }

    /**
     * Packs that shipped with the build or were sideloaded are not in the published catalog,
     * so a remote refresh would otherwise drop them and report an installed pack as missing.
     */
    private fun withBundledPacks(remote: List<ModelPack>): List<ModelPack> {
        val bundled = fs.readText(bundledPath())
            ?.let { runCatching { json.decodeFromString<PackManifest>(it) }.getOrNull() }
            ?.packs
            .orEmpty()
        val remoteIds = remote.map { it.id }.toSet()
        return remote + bundled.filter { it.id !in remoteIds }
    }

    private fun updateStatus(id: String, transform: (PackState) -> PackState) {
        val current = _states.value[id] ?: return
        _states.value = _states.value + (id to transform(current))
    }

    private fun versionDir(pack: ModelPack): String =
        "${fs.packsRoot()}/${pack.id}/${pack.version}"

    private fun cachePath(): String = "${fs.packsRoot()}/manifest.cache.json"

    private fun bundledPath(): String = "${fs.packsRoot()}/$BUNDLED_MANIFEST"

    private fun parentOf(path: String): String = path.substringBeforeLast('/')

    companion object {
        const val COMPLETE_MARKER = ".complete"

        /** Catalog entries for packs installed outside the published manifest. */
        const val BUNDLED_MANIFEST = "manifest.bundled.json"
        private const val SPACE_MARGIN_BYTES = 500L * 1024 * 1024
    }
}
