package com.zakir.vestra.shared.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.zakir.vestra.shared.engine.lite.LiteEngineIo
import com.zakir.vestra.shared.engine.lite.Watermark
import java.io.ByteArrayOutputStream

/**
 * Android IO for the cloud tier. Uploads are re-encoded JPEGs — this both
 * bounds the payload (long edge ≤ 1536 px) and strips EXIF (no GPS or device
 * metadata ever leaves the phone).
 */
class AndroidCloudIo(
    private val context: Context,
    private val liteIo: LiteEngineIo,
) : CloudIo {

    override fun isOnline(): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun readJpegBytes(uri: String): ByteArray? =
        liteIo.loadBitmap(uri)?.let(::toBoundedJpeg)

    override fun readAiModelJpegBytes(modelId: String): ByteArray? =
        liteIo.loadPerson(
            com.zakir.vestra.shared.domain.PersonSource.AiModel(modelId),
        )?.let(::toBoundedJpeg)

    override fun saveWatermarkedJpeg(bytes: ByteArray): String {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return liteIo.saveResult(Watermark.apply(bitmap))
    }

    private fun toBoundedJpeg(bitmap: Bitmap): ByteArray {
        val maxDim = maxOf(bitmap.width, bitmap.height)
        val scaled = if (maxDim > 1536) {
            val ratio = 1536f / maxDim
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * ratio).toInt(),
                (bitmap.height * ratio).toInt(),
                true,
            )
        } else {
            bitmap
        }
        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, out)
        return out.toByteArray()
    }
}
