package com.zakir.vestra.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

/**
 * Manual 90° rotation fallback for shop photos that carry no EXIF orientation
 * tag (EXIF auto-orientation can't fix those). Rotates the image, writes it to
 * app-private storage, and returns the new file URI so the engine reads the
 * corrected image.
 */
object ImageRotator {

    fun rotate90(context: Context, uri: String): String? = runCatching {
        val source = context.contentResolver.openInputStream(Uri.parse(uri))?.use {
            BitmapFactory.decodeStream(it)
        } ?: return null
        val matrix = Matrix().apply { postRotate(90f) }
        val rotated = Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)

        val dir = File(context.filesDir, "captures").apply { mkdirs() }
        val file = File(dir, "rotated_${System.currentTimeMillis()}.jpg")
        FileOutputStream(file).use { rotated.compress(Bitmap.CompressFormat.JPEG, 92, it) }
        "file://${file.absolutePath}"
    }.getOrNull()
}
