package com.zakir.vestra.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
 * Status text — "Blocked", "Verification failed", "Allowed".
 *
 * Six places styled these with `MaterialTheme.colorScheme.error`, which is Material's stock red.
 * On a violet-and-teal screen that red is the only colour from outside the system, so a status
 * line read as a rendering fault rather than as information. [VestraColors.Danger] is the
 * system's own alarm colour and still clears contrast on both palettes.
 */
object StatusColor {
    val bad: Color @Composable get() = VestraColors.Danger
    val good: Color @Composable get() = VestraColors.SaffronDeep
    val neutral: Color @Composable get() = VestraColors.InkMuted

    /** Success or failure in one call, for a `when` that would otherwise repeat the pair. */
    @Composable
    fun of(ok: Boolean): Color = if (ok) good else bad
}

/** Body copy tinted to the current surface. Kept beside the controls that pair with it. */
@Composable
fun mutedBodyColor(): Color = MaterialTheme.colorScheme.onSurfaceVariant
