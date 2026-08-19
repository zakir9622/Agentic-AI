package com.zakir.vestra.ui.screens.casting

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.domain.BodyType
import com.zakir.vestra.shared.domain.Ethnicity
import com.zakir.vestra.shared.domain.GarmentColor
import com.zakir.vestra.shared.domain.HairCoverage
import com.zakir.vestra.shared.domain.Scenario
import com.zakir.vestra.shared.domain.SkinTone
import com.zakir.vestra.ui.TryOnViewModel
import com.zakir.vestra.ui.theme.SpatialElevation

/** Spatial casting studio — parameter cards for ethnicity, body, scene, and color. */
@Composable
fun CastingStudioScreen(
    viewModel: TryOnViewModel,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val casting by viewModel.casting.collectAsState()

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("Casting", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Studio parameters", style = MaterialTheme.typography.headlineMedium)
                }
            }

            Spacer(Modifier.height(16.dp))

            SpatialParamCard(title = "Ethnicity", elevation = SpatialElevation.Raised) {
                ChipRow(Ethnicity.entries, casting.ethnicity) { viewModel.setEthnicity(it) }
            }

            Spacer(Modifier.height(12.dp))

            SpatialParamCard(title = "Skin tone", elevation = SpatialElevation.Floating) {
                ChipRow(SkinTone.entries, casting.skinTone) { viewModel.setSkinTone(it) }
            }

            Spacer(Modifier.height(12.dp))

            SpatialParamCard(title = "Body type", elevation = SpatialElevation.Raised) {
                ChipRow(BodyType.entries, casting.bodyType) { viewModel.setBodyType(it) }
            }

            Spacer(Modifier.height(12.dp))

            SpatialParamCard(title = "Hair coverage", elevation = SpatialElevation.Floating) {
                ChipRow(HairCoverage.entries, casting.hairCoverage) { viewModel.setHairCoverage(it) }
            }

            Spacer(Modifier.height(12.dp))

            SpatialParamCard(title = "Garment color", elevation = SpatialElevation.Raised) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        FilterChip(
                            selected = casting.garmentColor == null,
                            onClick = { viewModel.setGarmentColor(null) },
                            label = { Text("From photo") },
                        )
                    }
                    items(GarmentColor.entries) { color ->
                        FilterChip(
                            selected = casting.garmentColor == color,
                            onClick = { viewModel.setGarmentColor(color) },
                            label = { Text(color.displayName) },
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            SpatialParamCard(title = "Scenario", elevation = SpatialElevation.Floating) {
                ChipRow(Scenario.entries, casting.scenario) { viewModel.setScenario(it) }
            }

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = onNext,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
            ) {
                Text("Choose model & pose")
            }
        }
    }
}

@Composable
private fun SpatialParamCard(
    title: String,
    elevation: Float,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
                shadowElevation = elevation
            },
        shape = MaterialTheme.shapes.large,
        tonalElevation = elevation.dp,
        shadowElevation = elevation.dp,
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(10.dp))
            content()
        }
    }
}

@Composable
private inline fun <T> ChipRow(
    items: List<T>,
    selected: T,
    crossinline label: (T) -> String = { item ->
        when (item) {
            is Ethnicity -> item.displayName
            is SkinTone -> item.displayName
            is BodyType -> item.displayName
            is HairCoverage -> item.displayName
            is Scenario -> item.displayName
            else -> item.toString()
        }
    },
    crossinline onSelect: (T) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items(items) { item ->
            FilterChip(
                selected = item == selected,
                onClick = { onSelect(item) },
                label = { Text(label(item)) },
            )
        }
    }
}
