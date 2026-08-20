package com.zakir.vestra.shared.cloud

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.zakir.vestra.shared.domain.PersonSource
import com.zakir.vestra.shared.engine.lite.LiteEngineIo
import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
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

    override suspend fun downloadResult(urlOrPath: String): String {
        val bytes = when {
            urlOrPath.startsWith("data:image") -> {
                val b64 = urlOrPath.substringAfter("base64,")
                Base64.getDecoder().decode(b64)
            }
            urlOrPath.startsWith("http") -> http.get(urlOrPath).readBytes()
            urlOrPath.startsWith("/") -> {
                // Gradio local path on the Space host — fetch via URL if absolute
                if (urlOrPath.contains("hf.space")) http.get(urlOrPath).readBytes()
                else error("Local Gradio path cannot be fetched: $urlOrPath")
            }
            else -> {
                val file = File(urlOrPath)
                if (file.exists()) file.readBytes() else http.get(urlOrPath).readBytes()
            }
        }
        val dir = File(context.filesDir, "generations").apply { mkdirs() }
        val out = File(dir, "cloud_${System.currentTimeMillis()}.jpg")
        FileOutputStream(out).use { it.write(bytes) }
        return out.absolutePath
    }

    private fun Bitmap.toJpegBytes(): ByteArray {
        val stream = ByteArrayOutputStream()
        compress(Bitmap.CompressFormat.JPEG, 92, stream)
        return stream.toByteArray()
    }
}
