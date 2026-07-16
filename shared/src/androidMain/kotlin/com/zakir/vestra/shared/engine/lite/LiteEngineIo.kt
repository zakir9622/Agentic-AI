package com.zakir.vestra.shared.engine.lite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import com.zakir.vestra.shared.domain.PersonSource
import java.io.File
import java.io.FileOutputStream

/**
 * Image IO for the Lite engine. The AI-model base images are resolved through
 * [aiModelResolver] because the gallery (and its drawables) live in the app
 * module; user photos come from the content resolver.
 */
class LiteEngineIo(
    private val context: Context,
    private val aiModelResolver: (modelId: String) -> Bitmap?,
) {

    fun loadPerson(source: PersonSource): Bitmap? = when (source) {
        is PersonSource.UserPhoto -> loadBitmap(source.uri)
        is PersonSource.AiModel -> aiModelResolver(source.modelId)
    }

    fun loadBitmap(uri: String): Bitmap? = runCatching {
        val parsed = Uri.parse(uri)
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= MAX_DIMENSION) sample *= 2
        val options = BitmapFactory.Options().apply { inSampleSize = sample }
        resolver.openInputStream(parsed)?.use { BitmapFactory.decodeStream(it, null, options) }
    }.getOrNull()

    /** Writes the final JPEG and tags it as AI-generated in EXIF. */
    fun saveResult(image: Bitmap): String {
        val dir = File(context.filesDir, "generations").apply { mkdirs() }
        val file = File(dir, "tryon_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { image.compress(Bitmap.CompressFormat.JPEG, 94, it) }
        runCatching {
            ExifInterface(file.absolutePath).apply {
                setAttribute(ExifInterface.TAG_USER_COMMENT, Watermark.EXIF_TAG_VALUE)
                setAttribute(ExifInterface.TAG_SOFTWARE, "Vestra")
                saveAttributes()
            }
        }
        return file.absolutePath
    }

    private companion object {
        const val MAX_DIMENSION = 1600
    }
}
