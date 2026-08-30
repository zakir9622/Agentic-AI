package com.zakir.vestra.media

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.provider.MediaStore
import androidx.core.content.FileProvider
import androidx.exifinterface.media.ExifInterface
import com.zakir.vestra.shared.engine.lite.Watermark
import com.zakir.vestra.ui.components.GlassSnackbar
import com.zakir.vestra.ui.components.SnackbarLevel
import java.io.File
import java.io.FileOutputStream

/**
 * Unified save / share for try-on, Create, and Video outputs.
 * Gallery paths: DCIM/The Lookbook (images — Photos app), Movies/The Lookbook (video).
 */
object MediaExport {

    /** App album under DCIM so the system Photos app lists looks in their own folder. */
    const val IMAGE_ALBUM_RELATIVE_PATH = "DCIM/The Lookbook"
    const val VIDEO_ALBUM_RELATIVE_PATH = "Movies/The Lookbook"
    const val AUDIO_ALBUM_RELATIVE_PATH = "Music/The Lookbook"

    fun saveAudioToMusic(context: Context, file: File, title: String? = null, quiet: Boolean = false): Boolean {
        if (!file.exists()) {
            if (!quiet) GlassSnackbar.show("File missing", SnackbarLevel.ERROR)
            return false
        }
        val name = if (!title.isNullOrBlank()) {
            val knownExtensions = setOf(".mp3", ".wav", ".m4a", ".aac", ".ogg", ".flac")
            if (knownExtensions.any { title.endsWith(it, ignoreCase = true) }) {
                title
            } else {
                "$title.${file.extension}"
            }
        } else {
            file.name
        }
        val mime = when (file.extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "flac" -> "audio/flac"
            else -> "audio/wav"
        }
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, name)
            put(MediaStore.Audio.Media.MIME_TYPE, mime)
            put(MediaStore.Audio.Media.RELATIVE_PATH, AUDIO_ALBUM_RELATIVE_PATH)
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            if (!quiet) GlassSnackbar.show("Couldn't save to Music", SnackbarLevel.ERROR)
            return false
        }
        val written = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } != null
        }.getOrDefault(false)
        if (!written) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            if (!quiet) GlassSnackbar.show("Couldn't save to Music", SnackbarLevel.ERROR)
            return false
        }
        runCatching {
            val done = ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) }
            context.contentResolver.update(uri, done, null, null)
        }
        if (!quiet) {
            GlassSnackbar.show("Saved to Music · $AUDIO_ALBUM_RELATIVE_PATH", SnackbarLevel.SUCCESS)
        }
        return true
    }

    fun saveImageToGallery(context: Context, file: File, quiet: Boolean = false): Boolean {
        if (!file.exists()) {
            if (!quiet) GlassSnackbar.show("File missing", SnackbarLevel.ERROR)
            return false
        }
        Provenance.ensureImageFile(file, applyVisibleWatermark = false)
        val mime = when (file.extension.lowercase()) {
            "png" -> "image/png"
            else -> "image/jpeg"
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Images.Media.MIME_TYPE, mime)
            put(MediaStore.Images.Media.RELATIVE_PATH, IMAGE_ALBUM_RELATIVE_PATH)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            if (!quiet) GlassSnackbar.show("Couldn't save to Photos", SnackbarLevel.ERROR)
            return false
        }
        val written = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } != null
        }.getOrDefault(false)
        if (!written) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            if (!quiet) GlassSnackbar.show("Couldn't save to Photos", SnackbarLevel.ERROR)
            return false
        }
        runCatching {
            val done = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
            context.contentResolver.update(uri, done, null, null)
        }
        if (!quiet) {
            GlassSnackbar.show("Saved to Photos · $IMAGE_ALBUM_RELATIVE_PATH", SnackbarLevel.SUCCESS)
        }
        return true
    }

    fun saveVideoToGallery(context: Context, file: File, quiet: Boolean = false): Boolean {
        if (!file.exists()) {
            if (!quiet) GlassSnackbar.show("File missing", SnackbarLevel.ERROR)
            return false
        }
        val mime = when (file.extension.lowercase()) {
            "webm" -> "video/webm"
            else -> "video/mp4"
        }
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, mime)
            put(MediaStore.Video.Media.RELATIVE_PATH, VIDEO_ALBUM_RELATIVE_PATH)
            put(MediaStore.Video.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
        if (uri == null) {
            if (!quiet) GlassSnackbar.show("Couldn't save video", SnackbarLevel.ERROR)
            return false
        }
        val written = runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                file.inputStream().use { it.copyTo(out) }
            } != null
        }.getOrDefault(false)
        if (!written) {
            runCatching { context.contentResolver.delete(uri, null, null) }
            if (!quiet) GlassSnackbar.show("Couldn't save video", SnackbarLevel.ERROR)
            return false
        }
        runCatching {
            val done = ContentValues().apply { put(MediaStore.Video.Media.IS_PENDING, 0) }
            context.contentResolver.update(uri, done, null, null)
        }
        if (!quiet) {
            GlassSnackbar.show("Saved to $VIDEO_ALBUM_RELATIVE_PATH", SnackbarLevel.SUCCESS)
        }
        return true
    }

    fun saveAllImages(context: Context, files: List<File>): Int {
        var ok = 0
        files.forEach { if (saveImageToGallery(context, it, quiet = true)) ok++ }
        GlassSnackbar.show(
            when {
                ok == 0 -> "Couldn't save to Photos"
                ok == 1 -> "Saved to Photos · $IMAGE_ALBUM_RELATIVE_PATH"
                else -> "$ok shots saved to Photos · $IMAGE_ALBUM_RELATIVE_PATH"
            },
            if (ok > 0) SnackbarLevel.SUCCESS else SnackbarLevel.ERROR,
        )
        return ok
    }

    fun share(context: Context, file: File, chooserTitle: String = "Share") {
        if (!file.exists()) {
            GlassSnackbar.show("File missing", SnackbarLevel.ERROR)
            return
        }
        if (file.extension.lowercase() in setOf("jpg", "jpeg", "png")) {
            Provenance.ensureImageFile(file, applyVisibleWatermark = false)
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val type = when (file.extension.lowercase()) {
            "mp4", "webm" -> "video/${file.extension.lowercase()}"
            "png" -> "image/png"
            else -> "image/jpeg"
        }
        context.startActivity(
            Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    this.type = type
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                },
                chooserTitle,
            ),
        )
    }

    fun openVideo(context: Context, file: File) {
        if (!file.exists()) {
            GlassSnackbar.show("File missing", SnackbarLevel.ERROR)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(view) }
            .onFailure { share(context, file, "Share video") }
    }

    fun openAudio(context: Context, file: File) {
        if (!file.exists()) {
            GlassSnackbar.show("File missing", SnackbarLevel.ERROR)
            return
        }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val mime = when (file.extension.lowercase()) {
            "mp3" -> "audio/mpeg"
            "flac" -> "audio/flac"
            "ogg" -> "audio/ogg"
            else -> "audio/wav"
        }
        val view = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching { context.startActivity(view) }
            .onFailure { share(context, file, "Share audio") }
    }
}

/** Visible watermark + EXIF provenance for AI outputs (Play AI-content policy). */
object Provenance {
    fun ensureImageFile(file: File, applyVisibleWatermark: Boolean) {
        val ext = file.extension.lowercase()
        if (ext !in setOf("jpg", "jpeg", "png")) return
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
}
