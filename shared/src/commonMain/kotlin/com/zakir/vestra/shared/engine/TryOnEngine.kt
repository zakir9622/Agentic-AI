package com.zakir.vestra.shared.engine

import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.TryOnRequest
import kotlinx.coroutines.flow.Flow

/**
 * One implementation per tier (Lite, Pro, Cloud) plus a mock for development.
 * Implementations emit [GenerationState] updates and finish with either
 * [GenerationState.Complete] or [GenerationState.Failed] — they never throw
 * for expected failure modes.
 */
interface TryOnEngine {
    val tier: EngineTier

    /** Cheap synchronous check used by the router and by Settings to grey out tiers. */
    fun isAvailable(): Availability

    fun generate(request: TryOnRequest): Flow<GenerationState>
}

sealed interface Availability {
    data object Ready : Availability
    data class Unavailable(val reason: UnavailableReason) : Availability
}

enum class UnavailableReason {
    PACK_NOT_INSTALLED,
    DEVICE_NOT_CAPABLE,
    OFFLINE,
    NOT_CONFIGURED,
}
