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
 * AUTO: Pro if ready, else Lite. Cloud is never auto-selected (privacy invariant).
 */
class EngineRouter(private val engines: List<TryOnEngine>) {

    fun resolve(requested: EngineTier): TryOnEngine? = when (requested) {
        EngineTier.AUTO ->
            engineFor(EngineTier.PRO)?.takeIf { it.isAvailable() == Availability.Ready }
                ?: engineFor(EngineTier.LITE)?.takeIf { it.isAvailable() == Availability.Ready }
                ?: engineFor(EngineTier.LITE)
        else -> engineFor(requested)
    }

    fun generate(request: TryOnRequest): Flow<GenerationState> {
        val engine = resolve(request.tier)
            ?: return flowOf(GenerationState.Failed(TryOnError.Internal("No engine for tier ${request.tier}")))
        when (val availability = engine.isAvailable()) {
            is Availability.Ready -> Unit
            is Availability.Unavailable ->
                return flowOf(GenerationState.Failed(availability.reason.toError(engine.tier)))
        }
        return engine.generate(request)
    }

    fun availability(tier: EngineTier): Availability =
        engineFor(tier)?.isAvailable() ?: Availability.Unavailable(UnavailableReason.NOT_CONFIGURED)

    private fun engineFor(tier: EngineTier): TryOnEngine? = engines.firstOrNull { it.tier == tier }
}

private fun UnavailableReason.toError(tier: EngineTier): TryOnError = when (this) {
    UnavailableReason.PACK_VERIFY_FAILED ->
        TryOnError.Internal(
            when (tier) {
                EngineTier.PRO ->
                    "Pro pack failed verification — open Settings → Model packs and re-download " +
                        "pro-v2-int8 and lite-v1."
                EngineTier.LITE ->
                    "Lite pack failed verification — open Settings → Model packs and re-download lite-v1."
                else ->
                    "Model pack failed verification — re-download from Settings → Model packs."
            },
        )
    UnavailableReason.PACK_NOT_INSTALLED ->
        TryOnError.Internal(
            when (tier) {
                EngineTier.PRO ->
                    "Pro model pack not installed. Open Settings → Model packs to download pro-v2-int8 " +
                        "(~2 GB), and lite-v1 for human parsing."
                EngineTier.LITE ->
                    "Lite model pack not installed. Open Settings → Engines & packs to download lite-v1 " +
                        "(bundled in debug builds), or switch to Cloud try-on with an HF token."
                else ->
                    "Model pack not installed — open Settings → Model packs."
            },
        )
    UnavailableReason.COMPANION_PACK_MISSING ->
        TryOnError.Internal("Pro needs the Lite pack too — download lite-v1 in Settings → Model packs.")
    UnavailableReason.DEVICE_NOT_CAPABLE -> TryOnError.DeviceNotCapable
    UnavailableReason.OFFLINE -> TryOnError.NetworkUnavailable
    UnavailableReason.NOT_CONFIGURED -> TryOnError.Internal("Engine not configured — check Settings")
}
