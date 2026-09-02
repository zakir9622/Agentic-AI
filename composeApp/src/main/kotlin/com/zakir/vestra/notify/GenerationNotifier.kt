package com.zakir.vestra.notify

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.zakir.vestra.shared.cloud.AiCapability

/**
 * Posts "your generation finished" notifications.
 *
 * The app had exactly one notification channel before this — model-pack downloads, posted from
 * a foreground worker. Generations were silent, which matters because they are the long thing:
 * a cloud Space can queue for minutes and a cold LiteRT-LM engine takes tens of seconds, so
 * backgrounding the app mid-generation is the normal case, not an edge one.
 *
 * Three independent gates, all of which must pass, in cheapest-first order:
 * 1. the user's preference for this event kind ([AppSettingsGate]),
 * 2. the OS `POST_NOTIFICATIONS` grant,
 * 3. the app not currently being in the foreground — a result already visible on screen does
 *    not need an alert about itself.
 */
class GenerationNotifier(private val context: Context) {

    /** Set by the activity's lifecycle so [notifyComplete] can skip a visible result. */
    @Volatile
    var appInForeground: Boolean = true

    private val manager: NotificationManager?
        get() = ContextCompat.getSystemService(context, NotificationManager::class.java)

    private fun ensureChannel(): Boolean {
        val mgr = manager ?: return false
        // IMPORTANCE_DEFAULT, not HIGH: a finished generation is worth a sound, not a
        // heads-up card that interrupts whatever the user switched away to do.
        mgr.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Generation results", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Tells you when an image, video, clip or answer is ready." },
        )
        return true
    }

    private fun permitted(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    /** A generation finished successfully. No-op unless all three gates pass. */
    fun notifyComplete(capability: AiCapability, enabled: Boolean) {
        if (!enabled || appInForeground || !permitted() || !ensureChannel()) return
        post(
            id = NOTIFICATION_ID_COMPLETE,
            title = "${label(capability)} ready",
            body = "Tap to see it in The Lookbook.",
            icon = android.R.drawable.stat_sys_download_done,
        )
    }

    /** A generation failed. [reason] is truncated — a notification is not an error log. */
    fun notifyFailed(capability: AiCapability, reason: String, enabled: Boolean) {
        if (!enabled || appInForeground || !permitted() || !ensureChannel()) return
        post(
            id = NOTIFICATION_ID_FAILED,
            title = "${label(capability)} didn't finish",
            body = reason.take(120).ifBlank { "Tap to retry in The Lookbook." },
            icon = android.R.drawable.stat_notify_error,
        )
    }

    private fun post(id: Int, title: String, body: String, icon: Int) {
        val launch = context.packageManager.getLaunchIntentForPackage(context.packageName)
            ?.apply { flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP }
        val pending = launch?.let {
            PendingIntent.getActivity(
                context,
                id,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }
        val notification: Notification = Notification.Builder(context, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(icon)
            .setAutoCancel(true)
            .apply { pending?.let(::setContentIntent) }
            .build()
        // A revoked permission between the check above and here throws rather than failing
        // quietly; a missed notification must never take down a generation that succeeded.
        runCatching { manager?.notify(id, notification) }
    }

    private fun label(capability: AiCapability): String = when (capability) {
        AiCapability.IMAGE_GEN, AiCapability.IMAGE_EDIT, AiCapability.TRY_ON -> "Your image"
        AiCapability.VIDEO -> "Your clip"
        AiCapability.CODE -> "Your answer"
        AiCapability.AUDIO -> "Your audio"
    }

    private companion object {
        const val CHANNEL_ID = "generation_results"
        // Distinct ids so a failure does not silently replace an unread success.
        const val NOTIFICATION_ID_COMPLETE = 3001
        const val NOTIFICATION_ID_FAILED = 3002
    }
}
