package com.zakir.vestra.ui.components

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect as AndroidRect
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zakir.vestra.media.Provenance
import com.zakir.vestra.safety.BoxBlur
import com.zakir.vestra.safety.FaceBlurProcessor
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.theme.RadiusTokens
import com.zakir.vestra.ui.theme.SpacingTokens
import com.zakir.vestra.ui.theme.VestraColors
import java.io.File
import java.io.FileOutputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Privacy post-process for a generated image (B7): auto-blur any detected faces via
 * [FaceBlurProcessor] (fully offline, ML Kit), and/or draw manual blur regions for anything the
 * detector misses. Opens after generation, before the user decides what to keep.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PrivacyBlurSheet(
    imagePath: String,
    onDismiss: () -> Unit,
    onSaved: (savedPath: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VestraColors.SurfaceRaised,
    ) {
        PrivacyBlurContent(imagePath = imagePath, onSaved = onSaved)
    }
}

/**
 * The sheet's actual content, split out from [PrivacyBlurSheet] so it can be rendered and tested
 * directly — Robolectric's Compose test harness doesn't reliably dispatch click actions into a
 * live `ModalBottomSheet`'s window layer, so tests render this composable in a plain `Box`
 * instead of going through the sheet chrome. Production behavior is identical either way.
 */
@Composable
internal fun PrivacyBlurContent(
    imagePath: String,
    onSaved: (savedPath: String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()

    var autoBlur by remember { mutableStateOf(true) }
    var blurStrength by remember { mutableStateOf(25f) }
    var regions by remember { mutableStateOf<List<BlurRegion>>(emptyList()) }
    var displaySize by remember { mutableStateOf(IntSize.Zero) }
    var applying by remember { mutableStateOf(false) }
    var errorText by remember { mutableStateOf<String?>(null) }

    fun applyAndSave() {
        if (applying) return
        applying = true
        errorText = null
        scope.launch {
            val result = withContext(Dispatchers.Default) {
                runCatching {
                    val original = BitmapFactory.decodeFile(imagePath)
                        ?: error("Could not read the generated image.")
                    var bitmap = if (autoBlur) {
                        FaceBlurProcessor.detectAndBlur(original, blurStrength.toInt())
                    } else {
                        original.copy(Bitmap.Config.ARGB_8888, true)
                    }
                    if (regions.isNotEmpty() && displaySize.width > 0 && displaySize.height > 0) {
                        val scaleX = bitmap.width.toFloat() / displaySize.width
                        val scaleY = bitmap.height.toFloat() / displaySize.height
                        for (region in regions) {
                            val rect = AndroidRect(
                                (region.rect.left * scaleX).toInt(),
                                (region.rect.top * scaleY).toInt(),
                                (region.rect.right * scaleX).toInt(),
                                (region.rect.bottom * scaleY).toInt(),
                            )
                            BoxBlur.blurRegion(bitmap, rect, blurStrength.toInt())
                        }
                    }
                    val outFile = File(
                        File(imagePath).parentFile,
                        "privacy_blurred_${System.currentTimeMillis()}.jpg",
                    )
                    FileOutputStream(outFile).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 94, it) }
                    Provenance.ensureImageFile(outFile, applyVisibleWatermark = false)
                    outFile.absolutePath
                }
            }
            applying = false
            result.onSuccess { path -> onSaved(path) }
                .onFailure { err -> errorText = err.message?.take(120) ?: "Privacy blur failed" }
        }
    }

    Column(
        modifier
            .fillMaxWidth()
            .padding(horizontal = SpacingTokens.lg)
            .padding(bottom = SpacingTokens.xxl),
    ) {
        Text("Privacy blur", style = MaterialTheme.typography.titleMedium, color = VestraColors.Ink)
        Text(
            "Blurs faces automatically, fully offline. Draw extra regions for anything it misses.",
            style = MaterialTheme.typography.bodySmall,
            color = VestraColors.InkMuted,
            modifier = Modifier.padding(top = 4.dp, bottom = SpacingTokens.sm),
        )

        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(RadiusTokens.md))
                .background(VestraColors.GlassFill)
                .onSizeChanged { displaySize = it },
        ) {
            AsyncImage(
                model = File(imagePath),
                contentDescription = "Preview",
                modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                contentScale = ContentScale.Crop,
            )
            RegionBlurOverlay(
                regions = regions,
                onRegionsChange = { regions = it },
            )
        }

        Row(
            Modifier.fillMaxWidth().padding(top = SpacingTokens.sm),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Auto-blur faces", style = MaterialTheme.typography.bodyMedium, color = VestraColors.Ink)
            Switch(
                checked = autoBlur,
                onCheckedChange = { autoBlur = it },
                modifier = Modifier.testTag(TestTags.PRIVACY_BLUR_TOGGLE),
            )
        }

        Text("Blur strength", style = MaterialTheme.typography.labelMedium, color = VestraColors.InkMuted)
        Slider(
            value = blurStrength,
            onValueChange = { blurStrength = it },
            valueRange = 5f..50f,
        )

        if (regions.isNotEmpty()) {
            GlassSecondaryButton(
                text = "Clear drawn regions (${regions.size})",
                onClick = { regions = emptyList() },
            )
            Spacer(Modifier.height(SpacingTokens.xs))
        }

        if (errorText != null) {
            Text(errorText!!, style = MaterialTheme.typography.labelSmall, color = VestraColors.Danger)
            Spacer(Modifier.height(SpacingTokens.xs))
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(SpacingTokens.sm)) {
            GlassSecondaryButton(
                text = "Save original",
                onClick = { onSaved(imagePath) },
                modifier = Modifier.weight(1f).testTag(TestTags.PRIVACY_BLUR_SAVE_ORIGINAL),
            )
            if (applying) {
                Box(Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.testTag(TestTags.PRIVACY_BLUR_APPLY))
                }
            } else {
                GlassPrimaryButton(
                    text = "Save blurred",
                    onClick = { applyAndSave() },
                    modifier = Modifier.weight(1f).testTag(TestTags.PRIVACY_BLUR_APPLY),
                )
            }
        }
    }
}
