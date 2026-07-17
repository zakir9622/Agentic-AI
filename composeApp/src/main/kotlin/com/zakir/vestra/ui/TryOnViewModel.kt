package com.zakir.vestra.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.domain.GarmentCategory
import com.zakir.vestra.shared.domain.GarmentImage
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.PersonSource
import com.zakir.vestra.shared.domain.ShootState
import com.zakir.vestra.shared.domain.TryOnRequest
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.wardrobe.WardrobeEntry
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Holds the in-flight photoshoot: the garment, the cast (one or more person
 * sources = shots), and per-shot generation progress. Scoped to the whole nav
 * graph so the garment/casting/shoot/editorial screens share one instance.
 */
class TryOnViewModel(
    private val engineRouter: EngineRouter,
    private val appSettings: AppSettings,
    private val wardrobe: WardrobeRepository,
) : ViewModel() {

    private val _garment = MutableStateFlow<GarmentImage?>(null)
    val garment: StateFlow<GarmentImage?> = _garment

    private val _shots = MutableStateFlow<List<PersonSource>>(emptyList())
    val shots: StateFlow<List<PersonSource>> = _shots

    private val _backdrop = MutableStateFlow(com.zakir.vestra.shared.domain.Backdrop.STUDIO_WHITE)
    val backdrop: StateFlow<com.zakir.vestra.shared.domain.Backdrop> = _backdrop

    private val _shoot = MutableStateFlow<ShootState?>(null)
    val shoot: StateFlow<ShootState?> = _shoot

    private var shootJob: Job? = null

    fun selectGarment(uri: String) {
        _garment.value = GarmentImage(uri = uri, category = _garment.value?.category)
    }

    fun setCategory(category: GarmentCategory?) {
        _garment.value = _garment.value?.copy(category = category)
    }

    fun toggleShot(source: PersonSource) {
        val current = _shots.value
        _shots.value = when {
            current.contains(source) -> current - source
            // A user photo is a single-shot shoot; AI-model poses can stack.
            source is PersonSource.UserPhoto -> listOf(source)
            else -> current.filterIsInstance<PersonSource.AiModel>() + source
        }
    }

    fun setShots(sources: List<PersonSource>) {
        _shots.value = sources
    }

    fun setBackdrop(backdrop: com.zakir.vestra.shared.domain.Backdrop) {
        _backdrop.value = backdrop
    }

    @OptIn(ExperimentalUuidApi::class)
    fun startShoot() {
        val garment = _garment.value ?: return
        val shots = _shots.value.ifEmpty { return }
        shootJob?.cancel()
        val shootId = Uuid.random().toString()
        _shoot.value = ShootState(0, shots.size, GenerationState.Idle, emptyList())

        shootJob = viewModelScope.launch {
            val completed = mutableListOf<com.zakir.vestra.shared.domain.TryOnResult>()
            for ((index, person) in shots.withIndex()) {
                var failed = false
                engineRouter.generate(
                    TryOnRequest(
                        garment = garment,
                        person = person,
                        tier = appSettings.engineTier.value,
                        backdrop = _backdrop.value,
                    ),
                ).collect { state ->
                    if (state is GenerationState.Complete) {
                        completed += state.result
                        wardrobe.add(
                            WardrobeEntry(
                                id = Uuid.random().toString(),
                                createdAtEpochMillis = System.currentTimeMillis(),
                                imagePath = state.result.imagePath,
                                garmentUri = garment.uri,
                                personLabel = person.label(),
                                tier = state.result.executedTier,
                                shootId = shootId,
                            ),
                        )
                    }
                    if (state is GenerationState.Failed) failed = true
                    _shoot.value = ShootState(
                        shotIndex = index,
                        totalShots = shots.size,
                        inner = state,
                        completed = completed.toList(),
                    )
                }
                // A failed shot ends the shoot; completed shots stay saved.
                if (failed) return@launch
            }
        }
    }

    fun resetSession() {
        shootJob?.cancel()
        _garment.value = null
        _shots.value = emptyList()
        _shoot.value = null
    }
}

private fun PersonSource.label(): String = when (this) {
    is PersonSource.UserPhoto -> "Your photo"
    is PersonSource.AiModel -> "Studio model"
}
