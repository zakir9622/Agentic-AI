package com.zakir.vestra.shared.cloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CloudFailureTest {

    @Test
    fun routeUnsupportedAdvancesModel() {
        val failure = CloudFailureClassifier.fromMessage("HTTP 400: Model not supported by provider nscale")
        assertEquals(CloudFailure.RouteUnsupported, failure)
        assertTrue(failure.advanceModel)
        assertFalse(failure.retryVariants)
    }

    @Test
    fun safetyBlockedRetriesVariantsOnly() {
        val failure = CloudFailureClassifier.fromMessage("Content blocked by NSFW safety filter")
        assertEquals(CloudFailure.SafetyBlocked, failure)
        assertFalse(failure.advanceModel)
        assertTrue(failure.retryVariants)
    }

    @Test
    fun offlineShortCircuits() {
        val failure = CloudFailureClassifier.fromMessage("Unable to resolve host \"example.hf.space\"")
        assertEquals(CloudFailure.Offline, failure)
        assertFalse(failure.advanceModel)
    }

    @Test
    fun creditsExhaustedSkipsInferenceChain() {
        val failure = CloudFailureClassifier.fromMessage("HTTP 402: depleted your monthly Inference Providers credits")
        assertEquals(CloudFailure.CreditsExhausted, failure)
        assertTrue(failure.advanceModel)
    }
}
