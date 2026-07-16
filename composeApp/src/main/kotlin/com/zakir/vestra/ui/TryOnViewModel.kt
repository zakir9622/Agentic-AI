package com.zakir.vestra.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zakir.vestra.shared.domain.EngineTier
import com.zakir.vestra.shared.domain.GarmentImage
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.PersonSource
import com.zakir.vestra.shared.domain.TryOnRequest
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.wardrobe.WardrobeEntry
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Holds the in-flight try-on session: selected garment, selected person source,
 * and the generation state stream. Scoped to the whole nav graph so the
 * garment/person/generate/result screens share one instance.
 */
class TryOnViewModel(
    private val engineRouter: EngineRouter,
    private val appSettings: AppSettings,
    private val wardrobe: WardrobeRepository,
) : ViewModel() {

    private val _garment = MutableStateFlow<GarmentImage?>(null)
    val garment: StateFlow<GarmentImage?> = _garment

    private val _person = MutableStateFlow<PersonSource?>(null)
    val person: StateFlow<PersonSource?> = _person

    private val _generation = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generation: StateFlow<GenerationState> = _generation

    private var generationJob: Job? = null

    fun selectGarment(uri: String) {
        _garment.value = GarmentImage(uri = uri)
    }

    fun selectPerson(source: PersonSource) {
        _person.value = source
    }

    @OptIn(ExperimentalUuidApi::class)
    fun startGeneration() {
        val garment = _garment.value ?: return
        val person = _person.value ?: return
        generationJob?.cancel()
        _generation.value = GenerationState.Idle
        generationJob = viewModelScope.launch {
            engineRouter.generate(
                TryOnRequest(garment = garment, person = person, tier = appSettings.engineTier.value),
            ).onEach { state ->
                _generation.value = state
                if (state is GenerationState.Complete) {
                    wardrobe.add(
                        WardrobeEntry(
                            id = Uuid.random().toString(),
                            createdAtEpochMillis = System.currentTimeMillis(),
                            imagePath = state.result.imagePath,
                            garmentUri = garment.uri,
                            personLabel = person.label(),
                            tier = state.result.executedTier,
                        ),
                    )
                }
            }.collect {}
        }
    }

    fun resetSession() {
        generationJob?.cancel()
        _garment.value = null
        _person.value = null
        _generation.value = GenerationState.Idle
    }
}

private fun PersonSource.label(): String = when (this) {
    is PersonSource.UserPhoto -> "Your photo"
    is PersonSource.AiModel -> "AI model"
}
