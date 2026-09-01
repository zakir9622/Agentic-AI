package com.zakir.vestra.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import com.zakir.vestra.shared.safety.SafetyPreset
import com.zakir.vestra.ui.TestTags

/**
 * Confirm-before-generate gate for a safety preset whose [SafetyPreset.confirm] is true (Blur
 * identities, Redact details) — a real UI step for what that field's own doc comment promises,
 * shown by [com.zakir.vestra.ui.screens.home.UnifiedStudioPane] right before an image
 * generation would otherwise dispatch immediately.
 */
@Composable
fun SafetyConfirmDialog(
    preset: SafetyPreset,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text("Confirm: ${preset.label}") },
        text = { Text("The \"${preset.label}\" safety preset is active — ${preset.blurb}. Generate with this preset?") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag(TestTags.SAFETY_PRESET_CONFIRM_GENERATE),
            ) { Text("Generate") }
        },
        dismissButton = {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.testTag(TestTags.SAFETY_PRESET_CONFIRM_CANCEL),
            ) { Text("Cancel") }
        },
    )
}
