package com.zakir.vestra.shared.engine.litert

import com.zakir.vestra.shared.testutil.TestMemorySettings
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class LiteRtLmFailureStoreTest {

    private fun tempModelFile(bytes: Int): File =
        File.createTempFile("litertlm_failure_store_test", ".litertlm").apply {
            writeBytes(ByteArray(bytes))
            deleteOnExit()
        }

    @Test
    fun `unknown spec is not failed`() {
        val store = LiteRtLmFailureStore(TestMemorySettings())
        val file = tempModelFile(10)
        val spec = LiteRtLmEngineCache.EngineSpec(file.absolutePath, useGpu = false, visionEnabled = true, audioEnabled = false)
        assertNull(store.isKnownFailed(spec))
    }

    @Test
    fun `recorded failure is remembered while the file is unchanged`() {
        val store = LiteRtLmFailureStore(TestMemorySettings())
        val file = tempModelFile(10)
        val spec = LiteRtLmEngineCache.EngineSpec(file.absolutePath, useGpu = false, visionEnabled = true, audioEnabled = false)

        store.recordFailure(spec, "Vision Encoder model must have exactly one signature but got 2")

        assertEquals("Vision Encoder model must have exactly one signature but got 2", store.isKnownFailed(spec))
    }

    @Test
    fun `a changed file size self-invalidates the stale verdict`() {
        val store = LiteRtLmFailureStore(TestMemorySettings())
        val file = tempModelFile(10)
        val spec = LiteRtLmEngineCache.EngineSpec(file.absolutePath, useGpu = false, visionEnabled = true, audioEnabled = false)
        store.recordFailure(spec, "some deterministic failure")

        // Simulate a republish: same path, different (corrected) file contents/size.
        file.writeBytes(ByteArray(20))

        assertNull(store.isKnownFailed(spec))
    }

    @Test
    fun `different specs for the same model path are tracked independently`() {
        val store = LiteRtLmFailureStore(TestMemorySettings())
        val file = tempModelFile(10)
        val visionSpec = LiteRtLmEngineCache.EngineSpec(file.absolutePath, useGpu = false, visionEnabled = true, audioEnabled = false)
        val codeSpec = LiteRtLmEngineCache.EngineSpec(file.absolutePath, useGpu = false, visionEnabled = false, audioEnabled = false)

        store.recordFailure(visionSpec, "vision-only failure")

        assertEquals("vision-only failure", store.isKnownFailed(visionSpec))
        assertNull(store.isKnownFailed(codeSpec))
    }
}
