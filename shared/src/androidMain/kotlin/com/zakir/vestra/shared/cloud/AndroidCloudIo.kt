package com.zakir.vestra.shared.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.exifinterface.media.ExifInterface
import com.zakir.vestra.shared.domain.PersonSource
import com.zakir.vestra.shared.engine.lite.LiteEngineIo
import com.zakir.vestra.shared.engine.lite.Watermark
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import io.ktor.http.isSuccess
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Base64

class AndroidCloudIo(
    private val context: Context,
    private val liteIo: LiteEngineIo,
    private val http: HttpClient,
    private val applyVisibleWatermark: Boolean = true,
) : CloudImageIo {

    override suspend fun loadImageBytes(person: PersonSource): ByteArray? {
        val bitmap = liteIo.loadPerson(person) ?: return null
        return bitmap.toJpegBytes()
    }

    override suspend fun loadImageBytes(uri: String): ByteArray? {
        val bitmap = liteIo.loadBitmap(uri) ?: return null
        return bitmap.toJpegBytes()
    }

    override fun toDataUrl(jpegBytes: ByteArray): String =
        "data:image/jpeg;base64,${Base64.getEncoder().encodeToString(jpegBytes)}"

    override suspend fun downloadResult(urlOrPath: String, spaceHost: String?): String {
        val resolved = resolveUrl(urlOrPath, spaceHost)
        val bytes = when {
            resolved.startsWith("data:") -> {
                val b64 = resolved.substringAfter("base64,")
                Base64.getDecoder().decode(b64)
            }
            resolved.startsWith("http") -> fetchWithLegacyFallback(resolved)
            else -> {
                val file = File(resolved)
                if (file.exists()) file.readBytes()
                else error("Cannot fetch result: $urlOrPath")
            }
        }
        val isVideo = resolved.contains(".mp4", ignoreCase = true) ||
            resolved.contains(".webm", ignoreCase = true) ||
            bytesIsMp4(bytes)
        CloudOutputValidator.validate(bytes, isVideo = isVideo)?.let { reason ->
            error(reason)
        }
        val dir = File(context.filesDir, "generations").apply { mkdirs() }
        val ext = when {
            isVideo && (resolved.contains(".webm", ignoreCase = true) || isWebm(bytes)) -> "webm"
            isVideo -> "mp4"
            resolved.contains(".png", ignoreCase = true) -> "png"
            else -> "jpg"
        }
        val out = File(dir, "cloud_${System.currentTimeMillis()}.$ext")
        FileOutputStream(out).use { it.write(bytes) }
        if (ext in setOf("jpg", "jpeg", "png")) {
            stampProvenance(out, ext)
        }
        return out.absolutePath
    }

    /** Gradio 5 serves files under `/gradio_api/file=`; Gradio 4 serves them under `/file=`. */
    private suspend fun fetchWithLegacyFallback(url: String): ByteArray {
        val response = http.get(url)
        if (response.status.isSuccess()) return response.readRawBytes()
        if (response.status.value == 404 && url.contains("/gradio_api/file=")) {
            val legacy = url.replace("/gradio_api/file=", "/file=")
            val retry = http.get(legacy)
            if (retry.status.isSuccess()) return retry.readRawBytes()
        }
        error("Cannot fetch result (HTTP ${response.status.value}): $url")
    }

    private fun stampProvenance(file: File, ext: String) {
        runCatching {
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return
            val stamped = if (applyVisibleWatermark) Watermark.apply(bitmap) else bitmap
            val format = if (ext == "png") Bitmap.CompressFormat.PNG else Bitmap.CompressFormat.JPEG
            FileOutputStream(file).use { stamped.compress(format, 94, it) }
            if (ext != "png") {
                ExifInterface(file.absolutePath).apply {
                    setAttribute(ExifInterface.TAG_USER_COMMENT, Watermark.EXIF_TAG_VALUE)
                    setAttribute(ExifInterface.TAG_SOFTWARE, "The Lookbook")
                    saveAttributes()
                }
            }
            if (stamped !== bitmap) stamped.recycle()
            bitmap.recycle()
        }
    }

    /**
     * Gradio returns either a full `url` or a server-side `path`. A bare path must be fetched
     * through the Space's `file=` route — requesting the path directly 404s.
     */
    private fun resolveUrl(urlOrPath: String, spaceHost: String?): String {
        val trimmed = urlOrPath.trim()
        return when {
            trimmed.startsWith("http") || trimmed.startsWith("data:") -> trimmed
            spaceHost.isNullOrBlank() -> trimmed
            trimmed.startsWith("file=") ->
                "https://$spaceHost/gradio_api/file=${trimmed.removePrefix("file=")}"
            trimmed.startsWith("/") || trimmed.contains("/") ->
                "https://$spaceHost/gradio_api/file=$trimmed"
            else -> trimmed
        }
    }

    private fun bytesIsMp4(bytes: ByteArray): Boolean =
        bytes.size > 8 && bytes[4] == 'f'.code.toByte() && bytes[5] == 't'.code.toByte() &&
            bytes[6] == 'y'.code.toByte() && bytes[7] == 'p'.code.toByte()

    private fun isWebm(bytes: ByteArray): Boolean =
        bytes.size >= 4 && bytes[0] == 0x1A.toByte() && bytes[1] == 0x45.toByte() &&
            bytes[2] == 0xDF.toByte() && bytes[3] == 0xA3.toByte()

    private fun Bitmap.toJpegBytes(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 92, stream)
        return stream.toByteArray()
    }
}
