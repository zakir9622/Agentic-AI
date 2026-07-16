package com.zakir.vestra.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ImageDecoder
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.zakir.vestra.shared.domain.PersonSource
import com.zakir.vestra.shared.domain.TryOnRequest
import java.io.File
import java.io.FileOutputStream
import kotlin.math.min

/**
 * Output producer for the mock engine: composes the garment over the person at
 * torso height. Deliberately naive — it exists so the full UX (generation,
 * result, wardrobe, share) is real end-to-end before the Lite engine replaces
 * it in M3 with segmentation + warp on the same canvas.
 */
class MockCompositeRenderer(private val context: Context) {

    suspend fun render(request: TryOnRequest): String {
        val person = loadPerson(request.person)
        val garment = loadUri(Uri.parse(request.garment.uri))

        val canvasBitmap = person.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(canvasBitmap)

        // Place garment across the torso band: ~18%..62% of body height.
        val targetWidth = canvasBitmap.width * 0.52f
        val scale = min(targetWidth / garment.width, canvasBitmap.height * 0.44f / garment.height)
        val width = garment.width * scale
        val height = garment.height * scale
        val left = (canvasBitmap.width - width) / 2f
        val top = canvasBitmap.height * 0.18f
        val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply { alpha = 235 }
        canvas.drawBitmap(garment, null, RectF(left, top, left + width, top + height), paint)

        val outDir = File(context.filesDir, "generations").apply { mkdirs() }
        val outFile = File(outDir, "tryon_${System.currentTimeMillis()}.jpg")
        FileOutputStream(outFile).use { stream ->
            canvasBitmap.compress(Bitmap.CompressFormat.JPEG, 92, stream)
        }
        return outFile.absolutePath
    }

    private fun loadPerson(source: PersonSource): Bitmap = when (source) {
        is PersonSource.UserPhoto -> loadUri(Uri.parse(source.uri))
        is PersonSource.AiModel -> {
            val model = AiModelCatalog.byId(source.modelId) ?: AiModelCatalog.models.first()
            val drawable = requireNotNull(ContextCompat.getDrawable(context, model.image))
            val bitmap = createBitmap(900, 1200)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap
        }
    }

    private fun loadUri(uri: Uri): Bitmap {
        val source = ImageDecoder.createSource(context.contentResolver, uri)
        return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            decoder.isMutableRequired = false
            // Cap decode size — mock output doesn't need more than ~1600px.
            val maxDim = maxOf(info.size.width, info.size.height)
            if (maxDim > 1600) {
                val ratio = 1600f / maxDim
                decoder.setTargetSize(
                    (info.size.width * ratio).toInt(),
                    (info.size.height * ratio).toInt(),
                )
            }
        }
    }
}
