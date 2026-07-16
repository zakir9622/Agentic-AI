package com.zakir.vestra.shared.engine

import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.TryOnError
import com.zakir.vestra.shared.domain.TryOnRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Resolves the tier a request should run on and dispatches to that engine.
 *
 * AUTO policy: Pro if installed + capable, else Lite. Cloud is never chosen
 * implicitly — images only leave the device when the user explicitly selects
 * the Cloud tier (Play data-safety story depends on this).
 */
class EngineRouter(private val engines: List<TryOnEngine>) {

    fun resolve(requested: EngineTier): TryOnEngine? = when (requested) {
        EngineTier.AUTO ->
            engineFor(EngineTier.PRO)?.takeIf { it.isAvailable() == Availability.Ready }
                ?: engineFor(EngineTier.LITE)
        else -> engineFor(requested)
    }

    fun generate(request: TryOnRequest): Flow<GenerationState> {
        val engine = resolve(request.tier)
            ?: return flowOf(GenerationState.Failed(TryOnError.Internal("No engine for tier ${request.tier}")))
        when (val availability = engine.isAvailable()) {
            is Availability.Ready -> Unit
            is Availability.Unavailable ->
                return flowOf(GenerationState.Failed(availability.reason.toError()))
        }
        return engine.generate(request)
    }

    fun availability(tier: EngineTier): Availability =
        engineFor(tier)?.isAvailable() ?: Availability.Unavailable(UnavailableReason.NOT_CONFIGURED)

    private fun engineFor(tier: EngineTier): TryOnEngine? = engines.firstOrNull { it.tier == tier }
}

private fun UnavailableReason.toError(): TryOnError = when (this) {
    UnavailableReason.PACK_NOT_INSTALLED -> TryOnError.ModelPackMissing
    UnavailableReason.DEVICE_NOT_CAPABLE -> TryOnError.DeviceNotCapable
    UnavailableReason.OFFLINE -> TryOnError.NetworkUnavailable
    UnavailableReason.NOT_CONFIGURED -> TryOnError.Internal("Engine not configured")
}
