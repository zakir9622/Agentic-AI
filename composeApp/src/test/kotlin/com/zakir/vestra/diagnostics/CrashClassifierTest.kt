package com.zakir.vestra.diagnostics

import org.junit.Assert.assertTrue
import org.junit.Test

class CrashClassifierTest {

    @Test
    fun oomClassifies() {
        val cause = CrashClassifier.classify(OutOfMemoryError("Failed to allocate"))
        assertTrue(cause.contains("OutOfMemory"))
    }

    @Test
    fun ortClassifies() {
        val cause = CrashClassifier.classify(
            RuntimeException("session failed"),
            "at ai.onnxruntime.OrtSession.run\nat com.zakir.vestra.shared.engine.lite.OrtModel",
        )
        assertTrue(cause.contains("ONNX"))
    }

    @Test
    fun cancelClassifies() {
        val cause = CrashClassifier.classify(
            RuntimeException("StandaloneCoroutine was cancelled"),
            "cancellationexception in kotlinx.coroutines",
        )
        assertTrue(cause.contains("cancelled", ignoreCase = true))
    }
}
