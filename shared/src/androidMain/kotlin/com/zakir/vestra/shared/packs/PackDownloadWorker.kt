package com.zakir.vestra.shared.packs

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.request.header
import io.ktor.client.request.prepareGet
import io.ktor.client.statement.bodyAsChannel
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.utils.io.jvm.javaio.copyTo
import java.io.File
import java.io.FileOutputStream

/**
 * Resumable model-pack download. Each file is fetched with an HTTP Range
 * request continuing from whatever is already staged, so an interrupted
 * multi-gigabyte download picks up where it left off. Verification and the
 * atomic install commit happen in ModelPackManager.completeInstall.
 *
 * The host app must provide the manager via [PackDownloadWorker.dependencies].
 */
class PackDownloadWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val manager = dependencies?.invoke()
            ?: return Result.failure()
        val packId = inputData.getString(KEY_PACK_ID) ?: return Result.failure()
        manager.refresh(networkAllowed = true)
        val pack = manager.pack(packId) ?: return Result.failure()

        if (!manager.hasSpaceFor(pack)) {
            manager.markFailed(packId)
            return Result.failure(Data.Builder().putString(KEY_ERROR, "no_space").build())
        }

        setForeground(foregroundInfo(pack.displayName, 0))

        val stagingDir = File(manager.stagingDir(pack)).apply { mkdirs() }
        val http = HttpClient(OkHttp)
        try {
            var doneBytes = pack.files.sumOf { file ->
                File(stagingDir, file.path).takeIf { it.exists() }?.length() ?: 0L
            }
            for (file in pack.files) {
                val target = File(stagingDir, file.path).apply { parentFile?.mkdirs() }
                val existing = target.length()
                if (existing >= file.bytes) continue

                http.prepareGet(file.url) {
                    if (existing > 0) header(HttpHeaders.Range, "bytes=$existing-")
                }.execute { response ->
                    val resuming = response.status == HttpStatusCode.PartialContent
                    if (!resuming && existing > 0) target.delete()
                    FileOutputStream(target, resuming).use { out ->
                        response.bodyAsChannel().copyTo(out)
                    }
                }
                doneBytes += target.length() - existing
                val fraction = (doneBytes.toFloat() / pack.totalBytes).coerceIn(0f, 1f)
                manager.markDownloading(packId, fraction)
                setProgress(Data.Builder().putFloat(KEY_PROGRESS, fraction).build())
                setForeground(foregroundInfo(pack.displayName, (fraction * 100).toInt()))
            }

            return if (manager.completeInstall(packId, stagingDir.absolutePath)) {
                Result.success()
            } else {
                // Checksum mismatch: staged files are poisoned, start clean next time.
                stagingDir.deleteRecursively()
                Result.failure(Data.Builder().putString(KEY_ERROR, "checksum").build())
            }
        } catch (error: Exception) {
            manager.markFailed(packId)
            // Keep staging for resume; transient network errors retry.
            return if (runAttemptCount < MAX_RETRIES) Result.retry() else Result.failure()
        } finally {
            http.close()
        }
    }

    private fun foregroundInfo(packName: String, percent: Int): ForegroundInfo {
        val manager =
            applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Model downloads", NotificationManager.IMPORTANCE_LOW),
        )
        val notification: Notification = Notification.Builder(applicationContext, CHANNEL_ID)
            .setContentTitle("Downloading $packName")
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setProgress(100, percent, percent == 0)
            .setOngoing(true)
            .build()
        return if (Build.VERSION.SDK_INT >= 34) {
            ForegroundInfo(NOTIFICATION_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val KEY_PACK_ID = "pack_id"
        const val KEY_PROGRESS = "progress"
        const val KEY_ERROR = "error"
        private const val CHANNEL_ID = "pack_downloads"
        private const val NOTIFICATION_ID = 2001
        private const val MAX_RETRIES = 5

        /** Injected by the app's composition root before any work is enqueued. */
        var dependencies: (() -> ModelPackManager)? = null

        fun enqueue(context: Context, packId: String) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                "pack_download_$packId",
                ExistingWorkPolicy.KEEP,
                OneTimeWorkRequestBuilder<PackDownloadWorker>()
                    .setInputData(Data.Builder().putString(KEY_PACK_ID, packId).build())
                    .build(),
            )
        }
    }
}
