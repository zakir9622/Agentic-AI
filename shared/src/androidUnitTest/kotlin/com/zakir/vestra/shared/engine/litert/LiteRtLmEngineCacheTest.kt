package com.zakir.vestra.shared.engine.litert

import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Covers [LiteRtLmEngineCache.withNativeCallLock] — the mutex that serializes native calls
 * across different [LiteRtLmEngineCache.EngineSpec]s (the actual fix for the SIGSEGV crash: two
 * different `LiteRtLmEngine` instances issuing concurrent native JNI calls, since the vendored
 * SDK only synchronizes lifecycle calls per-instance, not across instances). Exercised directly
 * rather than through [LiteRtLmEngineCache.withEngine]/`withEngineFlow`, since those require a
 * real, natively-initialized `LiteRtLmEngine` that isn't constructible in a JVM unit test.
 */
class LiteRtLmEngineCacheTest {

    @Test
    fun concurrentCallsNeverOverlapInsideTheLock() = runTest {
        val activeCount = AtomicInteger(0)
        val maxObservedConcurrency = AtomicInteger(0)

        suspend fun contendingCall() = LiteRtLmEngineCache.withNativeCallLock {
            val active = activeCount.incrementAndGet()
            maxObservedConcurrency.updateAndGet { current -> maxOf(current, active) }
            delay(10)
            activeCount.decrementAndGet()
        }

        val first = async { contendingCall() }
        val second = async { contendingCall() }
        val third = async { contendingCall() }
        first.await()
        second.await()
        third.await()

        assertEquals(1, maxObservedConcurrency.get(), "the lock should never let two calls run at once")
    }

    @Test
    fun ordinaryAcquisitionSucceedsAndReturnsTheBlocksResult() = runTest {
        val result = LiteRtLmEngineCache.withNativeCallLock { "ok" }
        assertEquals("ok", result)
    }

    @Test
    fun exceptionInsideTheBlockStillReleasesTheLock() = runTest {
        assertFailsWith<IllegalStateException> {
            LiteRtLmEngineCache.withNativeCallLock { throw IllegalStateException("boom") }
        }
        // If the failed call above hadn't released the lock, this would hang until runTest's
        // virtual-time deadline and fail the test instead of returning normally.
        val result = LiteRtLmEngineCache.withNativeCallLock { "released" }
        assertEquals("released", result)
    }

    @Test
    fun aSecondCallerGivesUpInsteadOfHangingForeverBehindAStuckFirstCall() = runTest {
        // Hold the lock well past LiteRtLmEngine.INFERENCE_TIMEOUT_SEC (90s) — the second caller
        // must fail with a clear message rather than waiting indefinitely.
        val holder = async {
            LiteRtLmEngineCache.withNativeCallLock {
                delay((LiteRtLmEngine.INFERENCE_TIMEOUT_SEC + 30) * 1000L)
            }
        }
        delay(100) // let `holder` acquire the lock first

        val err = assertFailsWith<IllegalStateException> {
            LiteRtLmEngineCache.withNativeCallLock { "should not run" }
        }
        assertTrue(err.message!!.contains("busy"), "expected a clear busy message, got: ${err.message}")

        holder.cancel()
    }
}
