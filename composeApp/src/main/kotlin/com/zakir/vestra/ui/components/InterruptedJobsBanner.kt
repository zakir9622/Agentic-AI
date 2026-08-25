package com.zakir.vestra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.zakir.vestra.shared.jobs.LocalJobStore
import com.zakir.vestra.ui.TestTags
import com.zakir.vestra.ui.theme.VestraColors

/**
 * Surfaces local generations that were still [com.zakir.vestra.shared.jobs.LocalJobStatus.RUNNING]
 * the last time the app ran — the process was very likely reclaimed mid-generation (a Bonsai
 * Image 4B run is several minutes on CPU; backgrounding the app during one used to lose all
 * trace that anything had been asked for). This does not resume the generation — ONNX/LiteRT
 * sessions aren't checkpointable mid-run — it just tells the user what didn't finish, instead of
 * silently vanishing.
 *
 * Styled with the same warning accent + icon [GlassSnackbarCard] uses (not a plain [GlassCard],
 * whose neutral border made this blend into the background) so an interrupted job actually reads
 * as an alert.
 */
@Composable
fun InterruptedJobsBanner(localJobStore: LocalJobStore?) {
    if (localJobStore == null) return
    val jobs by localJobStore.jobs.collectAsState()
    val interrupted = jobs.filter {
        it.status == com.zakir.vestra.shared.jobs.LocalJobStatus.RUNNING ||
            it.status == com.zakir.vestra.shared.jobs.LocalJobStatus.QUEUED
    }
    if (interrupted.isEmpty()) return
    val shape = RoundedCornerShape(18.dp)
    Column(Modifier.fillMaxWidth().testTag(TestTags.INTERRUPTED_JOBS_BANNER)) {
        interrupted.forEach { job ->
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(shape)
                    .background(VestraColors.GlassFillStrong)
                    .border(1.dp, WarningAmberColor.copy(alpha = 0.5f), shape)
                    .padding(14.dp),
            ) {
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = WarningAmberColor,
                        modifier = Modifier.padding(top = 2.dp, end = 10.dp).size(20.dp),
                    )
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Interrupted: ${job.capability.lowercase().replace('_', ' ')}",
                            style = MaterialTheme.typography.titleSmall,
                            color = VestraColors.Ink,
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "\"${job.promptPreview}\" didn't finish — the app was likely closed " +
                                "mid-generation. Try again from the studio.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(
                        onClick = { localJobStore.dismiss(job.id) },
                        modifier = Modifier
                            .size(28.dp)
                            .testTag(TestTags.interruptedJobDismiss(job.id)),
                    ) {
                        Icon(
                            Icons.Outlined.Close,
                            contentDescription = "Dismiss",
                            tint = VestraColors.InkMuted,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
