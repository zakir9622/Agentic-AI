package com.zakir.vestra.shared.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.zakir.vestra.shared.domain.PersonSource
import com.zakir.vestra.shared.engine.lite.LiteEngineIo
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readRawBytes
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Base64

class AndroidCloudIo(
    private val context: Context,
    private val liteIo: LiteEngineIo,
    private val http: HttpClient,
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
            resolved.startsWith("http") -> http.get(resolved).readRawBytes()
            else -> {
                val file = File(resolved)
                if (file.exists()) file.readBytes()
                else error("Cannot fetch result: $urlOrPath")
            }
        }
        val dir = File(context.filesDir, "generations").apply { mkdirs() }
        val ext = when {
            resolved.contains(".mp4", ignoreCase = true) || bytesIsMp4(bytes) -> "mp4"
            resolved.contains(".webm", ignoreCase = true) -> "webm"
            resolved.contains(".png", ignoreCase = true) -> "png"
            else -> "jpg"
        }
        val out = File(dir, "cloud_${System.currentTimeMillis()}.$ext")
        FileOutputStream(out).use { it.write(bytes) }
        return out.absolutePath
    }

    private fun resolveUrl(urlOrPath: String, spaceHost: String?): String {
        val trimmed = urlOrPath.trim()
        return when {
            trimmed.startsWith("http") || trimmed.startsWith("data:") -> trimmed
            trimmed.startsWith("/") && !spaceHost.isNullOrBlank() ->
                "https://$spaceHost$trimmed"
            trimmed.startsWith("file=") && !spaceHost.isNullOrBlank() ->
                "https://$spaceHost/gradio_api/file=${trimmed.removePrefix("file=")}"
            !spaceHost.isNullOrBlank() && trimmed.contains("/") ->
                "https://$spaceHost/gradio_api/file=$trimmed"
            else -> trimmed
        }
    }

    private fun bytesIsMp4(bytes: ByteArray): Boolean =
        bytes.size > 8 && bytes[4] == 'f'.code.toByte() && bytes[5] == 't'.code.toByte()

    private fun Bitmap.toJpegBytes(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 92, stream)
        return stream.toByteArray()
    }
}
