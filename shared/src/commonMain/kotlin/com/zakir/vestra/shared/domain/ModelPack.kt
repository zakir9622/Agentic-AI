package com.zakir.vestra.shared.domain

import kotlinx.serialization.Serializable

/**
 * A downloadable bundle of model files hosted on Hugging Face Hub.
 * The manifest lives at `<packs repo>/manifest.json`; see ModelPackManager.
 */
@Serializable
data class ModelPack(
    val id: String,
    val version: Int,
    val tier: EngineTier,
    val displayName: String,
    val description: String,
    val totalBytes: Long,
    val files: List<PackFile>,
    val minSpec: DeviceSpec = DeviceSpec(),
    /**
     * True for packs built from non-commercially-licensed research weights.
     * Dev packs are for private testing only: they must never be listed in the
     * production manifest, and the UI labels them with [licenseNotice].
     */
    val devOnly: Boolean = false,
    val licenseNotice: String? = null,
    /** Whether this pack supplies an engine's models or the studio-model gallery. */
    val kind: PackKind = PackKind.ENGINE,
    /** For [PackKind.MODELS] packs: the base models this pack contributes. */
    val studioModels: List<StudioModelMeta> = emptyList(),
)

enum class PackKind { ENGINE, MODELS }

/** A base "studio model" image inside a MODELS pack. */
@Serializable
data class StudioModelMeta(
    val id: String,
    val displayName: String,
    /** File path within the pack, e.g. "front_hijab.jpg". */
    val file: String,
    /** Free-form tags for filtering, e.g. "hijab", "seated", "plus". */
    val tags: List<String> = emptyList(),
)

@Serializable
data class PackFile(
    /** Path relative to the pack root, e.g. "garment_seg_int8.tflite". */
    val path: String,
    val url: String,
    val sha256: String,
    val bytes: Long,
)

/** Minimum device requirements; empty = runs everywhere. */
@Serializable
data class DeviceSpec(
    val minRamMb: Long = 0,
    /** Requires an NNAPI/QNN-capable accelerator (Pro tier). */
    val requiresNpu: Boolean = false,
    val minSdk: Int = 26,
)

@Serializable
data class PackManifest(
    val schemaVersion: Int,
    val packs: List<ModelPack>,
)

enum class PackStatus { NOT_INSTALLED, DOWNLOADING, INSTALLED, UPDATE_AVAILABLE, INCOMPATIBLE }

data class PackState(
    val pack: ModelPack,
    val status: PackStatus,
    /** 0..1 while DOWNLOADING. */
    val progress: Float = 0f,
)
