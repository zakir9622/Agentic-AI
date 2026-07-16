package com.zakir.vestra.shared.packs

import com.zakir.vestra.shared.domain.DeviceSpec
import com.zakir.vestra.shared.domain.ModelPack
import com.zakir.vestra.shared.domain.PackManifest
import com.zakir.vestra.shared.domain.PackState
import com.zakir.vestra.shared.domain.PackStatus
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.json.Json

/**
 * Tracks which model packs exist, which are installed, and where their files
 * live. Downloading itself is platform work (WorkManager on Android) driven
 * through [markDownloading]/[completeInstall]; this class owns all state and
 * validation so that logic is shared and unit-testable.
 *
 * Layout: <packsRoot>/<packId>/<version>/<files…> with a `.complete` marker
 * written only after every file passed its sha256 check.
 */
class ModelPackManager(
    private val fs: PackFileSystem,
    private val device: DeviceProbe,
    private val http: HttpClient,
    private val manifestUrl: String,
) {
    private val json = Json { ignoreUnknownKeys = true }

    private val _states = MutableStateFlow<Map<String, PackState>>(emptyMap())
    val states: StateFlow<Map<String, PackState>> = _states

    /** Loads the last manifest fetched (offline start), then refreshes over the network. */
    suspend fun refresh(networkAllowed: Boolean = true) {
        cachedManifest()?.let { rebuildStates(it) }
        if (!networkAllowed) return
        runCatching {
            val response = http.get(manifestUrl)
            if (!response.status.isSuccess()) return
            val body = response.bodyAsText()
            json.decodeFromString<PackManifest>(body) // validate before persisting
            fs.mkdirs(fs.packsRoot())
            fs.writeText(cachePath(), body)
            rebuildStates(json.decodeFromString(body))
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

    fun markDownloading(id: String, progress: Float) {
        updateStatus(id) { it.copy(status = PackStatus.DOWNLOADING, progress = progress) }
    }

    fun markFailed(id: String) {
        updateStatus(id) { it.copy(status = PackStatus.NOT_INSTALLED, progress = 0f) }
    }

    /**
     * Called by the platform downloader once all files are staged. Verifies
     * every sha256 before committing; a corrupt file aborts the install.
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
        val target = versionDir(pack)
        fs.delete(target)
        fs.mkdirs(parentOf(target))
        fs.move(stagingDir, target)
        fs.writeText("$target/$COMPLETE_MARKER", pack.version.toString())
        // Older versions of this pack are dead weight now.
        fs.listFiles("${fs.packsRoot()}/${pack.id}")
            .filter { it != target }
            .forEach(fs::delete)
        updateStatus(id) { it.copy(status = PackStatus.INSTALLED, progress = 1f) }
        return true
    }

    fun uninstall(id: String) {
        fs.delete("${fs.packsRoot()}/$id")
        updateStatus(id) { it.copy(status = PackStatus.NOT_INSTALLED, progress = 0f) }
    }

    fun deviceMeets(spec: DeviceSpec): Boolean =
        device.totalRamMb() >= spec.minRamMb && (!spec.requiresNpu || device.hasNpu())

    /** True when the volume has room for the pack plus a safety margin. */
    fun hasSpaceFor(pack: ModelPack): Boolean =
        fs.freeBytes() > pack.totalBytes + SPACE_MARGIN_BYTES

    fun stagingDir(pack: ModelPack): String = "${fs.packsRoot()}/.staging/${pack.id}"

    private fun rebuildStates(manifest: PackManifest) {
        _states.value = manifest.packs.associate { pack ->
            val dir = versionDir(pack)
            val installedAnyVersion = fs.listFiles("${fs.packsRoot()}/${pack.id}")
                .any { fs.exists("$it/$COMPLETE_MARKER") }
            val status = when {
                fs.exists("$dir/$COMPLETE_MARKER") -> PackStatus.INSTALLED
                installedAnyVersion -> PackStatus.UPDATE_AVAILABLE
                !deviceMeets(pack.minSpec) -> PackStatus.INCOMPATIBLE
                else -> PackStatus.NOT_INSTALLED
            }
            pack.id to PackState(pack = pack, status = status)
        }
    }

    private fun cachedManifest(): PackManifest? =
        fs.readText(cachePath())?.let { cached ->
            runCatching { json.decodeFromString<PackManifest>(cached) }.getOrNull()
        }

    private fun updateStatus(id: String, transform: (PackState) -> PackState) {
        val current = _states.value[id] ?: return
        _states.value = _states.value + (id to transform(current))
    }

    private fun versionDir(pack: ModelPack): String =
        "${fs.packsRoot()}/${pack.id}/${pack.version}"

    private fun cachePath(): String = "${fs.packsRoot()}/manifest.cache.json"

    private fun parentOf(path: String): String = path.substringBeforeLast('/')

    companion object {
        const val COMPLETE_MARKER = ".complete"
        private const val SPACE_MARGIN_BYTES = 500L * 1024 * 1024
    }
}
