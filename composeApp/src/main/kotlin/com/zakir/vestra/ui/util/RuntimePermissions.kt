package com.zakir.vestra.ui.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.zakir.vestra.shared.packs.PackDownloadWorker

fun Context.hasPostNotificationsPermission(): Boolean =
    Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
        PackageManager.PERMISSION_GRANTED

fun Context.hasCameraPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
        PackageManager.PERMISSION_GRANTED

/**
 * Enqueues a pack download after optionally requesting [POST_NOTIFICATIONS] (API 33+).
 * Denial still starts the download — only the progress notification may be hidden.
 */
@Composable
fun rememberPackDownloadStarter(showToast: Boolean = true): (String) -> Unit {
    val context = LocalContext.current
    var pendingPackId by remember { mutableStateOf<String?>(null) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) {
        val id = pendingPackId
        pendingPackId = null
        if (id != null) enqueuePackDownload(context, id, showToast)
    }
    return { packId ->
        if (context.hasPostNotificationsPermission()) {
            enqueuePackDownload(context, packId, showToast)
        } else {
            pendingPackId = packId
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private fun enqueuePackDownload(context: Context, packId: String, showToast: Boolean) {
    PackDownloadWorker.enqueue(context, packId)
    if (showToast) {
        Toast.makeText(context, "Download started — resumes if interrupted", Toast.LENGTH_SHORT).show()
    }
}

/**
 * Requests [CAMERA] then runs [onGranted], or [onDenied] if refused.
 */
@Composable
fun rememberCameraGatedAction(onGranted: () -> Unit, onDenied: () -> Unit = {}): () -> Unit {
    val context = LocalContext.current
    var awaiting by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (!awaiting) return@rememberLauncherForActivityResult
        awaiting = false
        if (granted) onGranted() else onDenied()
    }
    return {
        if (context.hasCameraPermission()) {
            onGranted()
        } else {
            awaiting = true
            launcher.launch(Manifest.permission.CAMERA)
        }
    }
}
