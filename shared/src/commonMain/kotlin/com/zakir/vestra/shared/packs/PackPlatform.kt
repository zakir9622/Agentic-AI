package com.zakir.vestra.shared.packs

/**
 * Platform seams the pack manager needs. Android implementations use
 * java.io/MessageDigest/ActivityManager; iOS will use NSFileManager/CryptoKit.
 * Interfaces (not expect/actual) so tests can fake them.
 */
interface PackFileSystem {
    /** Absolute path of the root directory packs are installed under. */
    fun packsRoot(): String
    fun exists(path: String): Boolean
    fun fileSize(path: String): Long
    fun delete(path: String)
    fun mkdirs(path: String)
    /** Atomic rename; falls back to copy+delete across filesystems. */
    fun move(from: String, to: String)
    fun sha256(path: String): String
    fun listFiles(path: String): List<String>
    fun readText(path: String): String?
    fun writeText(path: String, content: String)
    /** Free bytes available on the volume holding the packs root. */
    fun freeBytes(): Long
}

/** Hardware capability probe used to gate packs (Pro tier needs RAM + NPU). */
interface DeviceProbe {
    fun totalRamMb(): Long
    /** Best-effort: true when an NNAPI/QNN-class accelerator is likely present. */
    fun hasNpu(): Boolean
}
