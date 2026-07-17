package com.zakir.vestra.shared.engine.lite

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
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
        val decoded = resolver.openInputStream(parsed)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return@runCatching null
        // Real shop photos are frequently shot sideways; honor the EXIF
        // orientation so the model and garment aren't fed a rotated image.
        applyExifOrientation(resolver, parsed, decoded)
    }.getOrNull()

    private fun applyExifOrientation(
        resolver: android.content.ContentResolver,
        uri: Uri,
        bitmap: Bitmap,
    ): Bitmap {
        val orientation = runCatching {
            resolver.openInputStream(uri)?.use { stream ->
                ExifInterface(stream).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL,
                )
            }
        }.getOrNull() ?: ExifInterface.ORIENTATION_NORMAL

        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> { matrix.postRotate(90f); matrix.postScale(-1f, 1f) }
            ExifInterface.ORIENTATION_TRANSVERSE -> { matrix.postRotate(270f); matrix.postScale(-1f, 1f) }
            else -> return bitmap
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

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
