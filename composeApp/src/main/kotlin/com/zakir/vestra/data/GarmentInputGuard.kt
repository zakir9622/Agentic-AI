package com.zakir.vestra.data

import com.zakir.vestra.shared.engine.lite.HumanParsing
import com.zakir.vestra.shared.engine.lite.LiteEngineIo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Flags when a chosen "garment" is actually a photo of a person wearing the
 * outfit (like the finished model shots that made the Lite compositor paste the
 * whole picture onto the model). Runs off the main thread; returns false when it
 * can't decide (e.g. Lite pack not installed) so it never blocks the flow.
 */
class GarmentInputGuard(
    private val io: LiteEngineIo,
    private val parsing: HumanParsing,
) {
    suspend fun looksLikeWornPhoto(uri: String): Boolean = withContext(Dispatchers.Default) {
        val bitmap = io.loadBitmap(uri) ?: return@withContext false
        runCatching { parsing.looksLikeWornPhoto(bitmap) }.getOrDefault(false)
    }
}
