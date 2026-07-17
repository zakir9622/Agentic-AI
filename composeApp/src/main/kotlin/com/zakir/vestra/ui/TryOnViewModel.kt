package com.zakir.vestra.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zakir.vestra.shared.domain.Backdrop
import com.zakir.vestra.shared.domain.GarmentCategory
import com.zakir.vestra.shared.domain.GarmentImage
import com.zakir.vestra.shared.domain.GenerationState
import com.zakir.vestra.shared.domain.PersonSource
import com.zakir.vestra.shared.domain.ShootState
import com.zakir.vestra.shared.domain.TryOnRequest
import com.zakir.vestra.shared.domain.TryOnResult
import com.zakir.vestra.shared.domain.layerRank
import com.zakir.vestra.shared.engine.EngineRouter
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.shared.wardrobe.WardrobeEntry
import com.zakir.vestra.shared.wardrobe.WardrobeRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Holds the in-flight photoshoot: the outfit (one or more garment pieces — a
 * South Asian suit is trousers + kurta + dupatta), the cast (one or more person
 * sources = shots), and per-shot progress. For each shot the pieces are layered
 * onto the model in [layerRank] order by chaining: each layer's output becomes
 * the next layer's input, so a full outfit renders in a single shot.
 */
class TryOnViewModel(
    private val engineRouter: EngineRouter,
    private val appSettings: AppSettings,
    private val wardrobe: WardrobeRepository,
) : ViewModel() {

    private val _outfit = MutableStateFlow<List<GarmentImage>>(emptyList())
    val outfit: StateFlow<List<GarmentImage>> = _outfit

    private val _shots = MutableStateFlow<List<PersonSource>>(emptyList())
    val shots: StateFlow<List<PersonSource>> = _shots

    private val _backdrop = MutableStateFlow(Backdrop.STUDIO_WHITE)
    val backdrop: StateFlow<Backdrop> = _backdrop

    private val _shoot = MutableStateFlow<ShootState?>(null)
    val shoot: StateFlow<ShootState?> = _shoot

    private var shootJob: Job? = null

    // ── outfit building ────────────────────────────────────────────────────────

    fun addGarment(uri: String) {
        _outfit.value = _outfit.value + GarmentImage(uri = uri)
    }

    fun removeGarment(index: Int) {
        _outfit.value = _outfit.value.filterIndexed { i, _ -> i != index }
    }

    fun setGarmentCategory(index: Int, category: GarmentCategory?) {
        _outfit.value = _outfit.value.mapIndexed { i, piece ->
            if (i == index) piece.copy(category = category) else piece
        }
    }

    /** Replaces a piece's image (used after manual rotation), keeping its category. */
    fun setGarmentUri(index: Int, uri: String) {
        _outfit.value = _outfit.value.mapIndexed { i, piece ->
            if (i == index) piece.copy(uri = uri) else piece
        }
    }

    fun setShots(sources: List<PersonSource>) {
        _shots.value = sources
    }

    fun toggleShot(source: PersonSource) {
        val current = _shots.value
        _shots.value = when {
            current.contains(source) -> current - source
            source is PersonSource.UserPhoto -> listOf(source)
            else -> current.filterIsInstance<PersonSource.AiModel>() + source
        }
    }

    fun setBackdrop(backdrop: Backdrop) {
        _backdrop.value = backdrop
    }

    // ── shoot ──────────────────────────────────────────────────────────────────

    @OptIn(ExperimentalUuidApi::class)
    fun startShoot() {
        val outfit = _outfit.value.ifEmpty { return }
        val shots = _shots.value.ifEmpty { return }
        // Trousers first, kurta next, dupatta on top.
        val layers = outfit.sortedBy { it.category.layerRank() }
        shootJob?.cancel()
        val shootId = Uuid.random().toString()
        _shoot.value = ShootState(0, shots.size, GenerationState.Idle, emptyList())

        shootJob = viewModelScope.launch {
            val completed = mutableListOf<TryOnResult>()
            for ((shotIndex, person) in shots.withIndex()) {
                val shotResult = renderShot(person, layers, shotIndex, shots.size, completed)
                    ?: return@launch // a failed layer ends the shoot; earlier shots stay saved
                completed += shotResult
                wardrobe.add(
                    WardrobeEntry(
                        id = Uuid.random().toString(),
                        createdAtEpochMillis = System.currentTimeMillis(),
                        imagePath = shotResult.imagePath,
                        garmentUri = outfit.first().uri,
                        personLabel = person.label(),
                        tier = shotResult.executedTier,
                        shootId = shootId,
                    ),
                )
                _shoot.value = ShootState(shotIndex, shots.size, GenerationState.Complete(shotResult), completed.toList())
            }
        }
    }

    /** Chains every outfit layer onto [person]; returns the final composited shot, or null on failure. */
    private suspend fun renderShot(
        person: PersonSource,
        layers: List<GarmentImage>,
        shotIndex: Int,
        totalShots: Int,
        completed: List<TryOnResult>,
    ): TryOnResult? {
        var currentPerson = person
        var lastResult: TryOnResult? = null
        layers.forEachIndexed { layerIndex, piece ->
            val isLastLayer = layerIndex == layers.lastIndex
            val terminal = engineRouter.generate(
                TryOnRequest(
                    garment = piece,
                    person = currentPerson,
                    tier = appSettings.engineTier.value,
                    // Stage the backdrop only on the final layer; intermediate
                    // layers keep the original scene so the next layer segments cleanly.
                    backdrop = if (isLastLayer) _backdrop.value else Backdrop.ORIGINAL,
                ),
            ).onEachReport(shotIndex, totalShots, layerIndex, layers.size, completed).last()

            when (terminal) {
                is GenerationState.Complete -> {
                    lastResult = terminal.result
                    // Feed this layer's output forward as the person for the next piece.
                    currentPerson = PersonSource.UserPhoto("file://${terminal.result.imagePath}")
                }
                is GenerationState.Failed -> {
                    _shoot.value = ShootState(shotIndex, totalShots, terminal, completed)
                    return null
                }
                else -> Unit
            }
        }
        return lastResult
    }

    /** Republishes each engine state as shoot progress, folding layer into the label. */
    private fun Flow<GenerationState>.onEachReport(
        shotIndex: Int,
        totalShots: Int,
        layerIndex: Int,
        layerCount: Int,
        completed: List<TryOnResult>,
    ): Flow<GenerationState> = onEach { state ->
        val labelled = if (layerCount > 1 && state is GenerationState.Running) {
            state.copy(stage = "Layer ${layerIndex + 1}/$layerCount · ${state.stage}")
        } else {
            state
        }
        // Don't surface a layer's Complete as the shot's Complete — the shoot
        // loop emits the shot Complete once all layers finish.
        val forShoot = if (labelled is GenerationState.Complete && layerIndex < layerCount - 1) {
            GenerationState.Running(1f, "Layer ${layerIndex + 1} done")
        } else {
            labelled
        }
        _shoot.value = ShootState(shotIndex, totalShots, forShoot, completed)
    }

    fun resetSession() {
        shootJob?.cancel()
        _outfit.value = emptyList()
        _shots.value = emptyList()
        _shoot.value = null
    }
}

private fun PersonSource.label(): String = when (this) {
    is PersonSource.UserPhoto -> "Your photo"
    is PersonSource.AiModel -> "Studio model"
}
