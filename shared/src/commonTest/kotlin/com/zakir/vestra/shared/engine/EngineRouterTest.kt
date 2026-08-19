package com.zakir.vestra.shared.engine

import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.domain.GarmentImage
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.PersonSource
import com.zakir.vestra.shared.domain.TryOnError
import com.zakir.vestra.shared.domain.TryOnRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

private class FakeEngine(
    override val tier: EngineTier,
    private val availability: Availability = Availability.Ready,
) : TryOnEngine {
    override fun isAvailable(): Availability = availability
    override fun generate(request: TryOnRequest): Flow<GenerationState> = emptyFlow()
}

class EngineRouterTest {

    private fun request(tier: EngineTier) = TryOnRequest(
        garment = GarmentImage(uri = "file:///garment.jpg"),
        person = PersonSource.AiModel("base-01"),
        tier = tier,
    )

    @Test
    fun autoPrefersProWhenReady() {
        val pro = FakeEngine(EngineTier.PRO)
        val lite = FakeEngine(EngineTier.LITE)
        val router = EngineRouter(listOf(lite, pro))
        assertEquals(pro, router.resolve(EngineTier.AUTO))
    }

    @Test
    fun autoFallsBackToLiteWhenProUnavailable() {
        val pro = FakeEngine(EngineTier.PRO, Availability.Unavailable(UnavailableReason.PACK_NOT_INSTALLED))
        val lite = FakeEngine(EngineTier.LITE)
        val router = EngineRouter(listOf(lite, pro))
        assertEquals(lite, router.resolve(EngineTier.AUTO))
    }

    @Test
    fun unavailableEngineFailsWithMappedError() = runTest {
        val pro = FakeEngine(EngineTier.PRO, Availability.Unavailable(UnavailableReason.PACK_NOT_INSTALLED))
        val router = EngineRouter(listOf(pro))
        val terminal = router.generate(request(EngineTier.PRO)).last()
        val failed = assertIs<GenerationState.Failed>(terminal)
        assertEquals(TryOnError.ModelPackMissing, failed.error)
    }

    @Test
    fun missingEngineFailsInsteadOfThrowing() = runTest {
        val router = EngineRouter(emptyList())
        val terminal = router.generate(request(EngineTier.LITE)).last()
        assertIs<GenerationState.Failed>(terminal)
    }
}
