package com.zakir.vestra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.zakir.vestra.ui.theme.VestraColors

/**
 * The app's switch.
 *
 * Every toggle in the app used a bare Material 3 [Switch] with default colours, and not one of
 * the six call sites passed a palette. Those defaults derive from `colorScheme.surfaceVariant`
 * and `outline`, which in this violet system land within a few percent of the white card a
 * settings toggle sits on — so in light mode the off state read as a pale blob with no border
 * and off was nearly indistinguishable from on. A control whose entire job is to show one bit of
 * state was showing none.
 *
 * The rule the colours follow: **on is accent-filled, off is outlined.** Fill versus no-fill is a
 * difference that survives a glance, a dim screen, and colour-blindness; hue alone is none of
 * those things.
 */
@Composable
fun VestraSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Switch(
        checked = checked,
        onCheckedChange = onCheckedChange,
        modifier = modifier,
        enabled = enabled,
        colors = vestraSwitchColors(),
    )
}

/** Shared so a `Switch` written inline still gets the palette. */
@Composable
fun vestraSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = VestraColors.Ivory,
    checkedTrackColor = VestraColors.Accent,
    checkedBorderColor = VestraColors.Accent,
    // Off is a hollow track with a visible rim, not a lighter fill. On a white card the fill
    // approach is what disappeared.
    uncheckedThumbColor = VestraColors.InkMuted,
    uncheckedTrackColor = Color.Transparent,
    uncheckedBorderColor = VestraColors.InkMuted.copy(alpha = 0.55f),
    disabledCheckedThumbColor = VestraColors.Ivory.copy(alpha = 0.6f),
    disabledCheckedTrackColor = VestraColors.Accent.copy(alpha = 0.4f),
    disabledUncheckedThumbColor = VestraColors.InkMuted.copy(alpha = 0.4f),
    disabledUncheckedTrackColor = Color.Transparent,
    disabledUncheckedBorderColor = VestraColors.InkMuted.copy(alpha = 0.25f),
)

/**
 * A small tinted capsule for a one-word state — "Ready", "Degraded", "Unsupported".
 *
 * Written for the Help screen's model-readiness list, which rendered twenty-two rows of
 * `"name · Status · schema note"` as one grey text style. The status was the only part a
 * reader scans for and it was the hardest part to find. The chip pulls it out of the
 * sentence and colours it, so the list can be read by shape instead of word by word.
 *
 * Tinted fill rather than a solid one: a row of saturated badges down the left edge of a
 * long list is louder than the content it labels.
 */
@Composable
fun StatusChip(text: String, tone: StatusTone, modifier: Modifier = Modifier) {
    val color = tone.color
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .border(1.dp, color.copy(alpha = 0.35f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/**
 * The good / warning / bad triple, resolved against the app palette rather than Material's.
 *
 * `MaterialTheme.colorScheme.error` is stock red; on a violet-and-teal screen it is the only
 * colour from outside the system, so an error line reads as a rendering fault rather than as
 * information. These three are the system's own.
 */
enum class StatusTone {
    GOOD,
    WARN,
    BAD,
    ;

    val color: Color
        @Composable get() = when (this) {
            GOOD -> VestraColors.Success
            WARN -> VestraColors.Warning
            BAD -> VestraColors.Danger
        }
}
