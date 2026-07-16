package com.zakir.vestra.ui.screens.person

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddAPhoto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.zakir.vestra.data.AiModelCatalog
import com.zakir.vestra.shared.domain.PersonSource
import com.zakir.vestra.shared.settings.AppSettings
import com.zakir.vestra.ui.TryOnViewModel

/**
 * "Who wears it": the AI-model gallery, or the user's own photo.
 * First use of a personal photo requires the likeness-consent acknowledgement
 * (Play AI-content policy; see docs/PLAY_COMPLIANCE.md).
 */
@Composable
fun PersonSourceScreen(
    viewModel: TryOnViewModel,
    appSettings: AppSettings,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    val person by viewModel.person.collectAsState()
    val consentAccepted by appSettings.likenessConsentAccepted.collectAsState()
    var showConsentDialog by remember { mutableStateOf(false) }

    val pickLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        uri?.let { viewModel.selectPerson(PersonSource.UserPhoto(it.toString())) }
    }

    fun requestUserPhoto() {
        if (consentAccepted) {
            pickLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        } else {
            showConsentDialog = true
        }
    }

    if (showConsentDialog) {
        AlertDialog(
            onDismissRequest = { showConsentDialog = false },
            title = { Text("Using personal photos") },
            text = {
                Text(
                    "Only use photos of yourself, or of people who have given you " +
                        "permission. Generated images are watermarked as AI-created. " +
                        "Creating images of someone without their consent may be unlawful.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        appSettings.setLikenessConsentAccepted()
                        showConsentDialog = false
                        pickLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                ) { Text("I understand") }
            },
            dismissButton = {
                TextButton(onClick = { showConsentDialog = false }) { Text("Cancel") }
            },
        )
    }

    Surface(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
                }
                Column {
                    Text("Act II", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text("Who wears it", style = MaterialTheme.typography.headlineMedium)
                }
            }

            Spacer(Modifier.height(16.dp))

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    PersonCard(
                        selected = person is PersonSource.UserPhoto,
                        onClick = ::requestUserPhoto,
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                            Icon(
                                Icons.Outlined.AddAPhoto,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text("Your photo", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Full-body works best",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                items(AiModelCatalog.models, key = { it.id }) { model ->
                    val isSelected = (person as? PersonSource.AiModel)?.modelId == model.id
                    PersonCard(
                        selected = isSelected,
                        onClick = { viewModel.selectPerson(PersonSource.AiModel(model.id)) },
                    ) {
                        Column(Modifier.fillMaxSize()) {
                            Image(
                                painter = painterResource(model.image),
                                contentDescription = model.displayName,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentScale = ContentScale.Crop,
                            )
                            Text(
                                model.displayName,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onNext,
                enabled = person != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            ) {
                Text("Create the look")
            }
        }
    }
}

@Composable
private fun PersonCard(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    }
    Surface(
        modifier = Modifier
            .aspectRatio(0.75f)
            .clip(RoundedCornerShape(16.dp))
            .border(if (selected) 2.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        content()
    }
}
