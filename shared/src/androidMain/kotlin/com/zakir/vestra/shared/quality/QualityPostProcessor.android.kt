package com.zakir.vestra.shared.quality

import com.zakir.vestra.shared.packs.ModelPackManager
import java.io.File

actual fun createQualityPostProcessor(packs: ModelPackManager): QualityPostProcessor =
    AndroidQualityPostProcessor(packs)

/**
 * Android quality post-processor. Runs Real-ESRGAN / BiRefNet when their
 * packs are installed; otherwise returns null and callers keep the original image.
 */
class AndroidQualityPostProcessor(
    private val packs: ModelPackManager,
) : QualityPostProcessor {

    override fun upscaleIfAvailable(rgba: ByteArray, width: Int, height: Int): ProcessedImage? {
        val dir = packs.installedDir(REALESRGAN_PACK) ?: return null
        val model = File(dir).listFiles()?.firstOrNull { it.name.endsWith(".onnx") } ?: return null
        return runCatching {
            QualityOnnxUpscaler(model.absolutePath).upscale(rgba, width, height)
        }.getOrNull()
    }

    override fun refineMatteIfAvailable(rgba: ByteArray, width: Int, height: Int): ByteArray? {
        val dir = packs.installedDir(BIREFNET_PACK) ?: return null
        val model = File(dir).listFiles()?.firstOrNull { it.name.endsWith(".onnx") } ?: return null
        return runCatching {
            QualityOnnxMatte(model.absolutePath).refine(rgba, width, height)
        }.getOrNull()
    }

    private companion object {
        const val REALESRGAN_PACK = "realesrgan-v1"
        const val BIREFNET_PACK = "birefnet-v1"
    }
}

/**
 * Real-ESRGAN ONNX upscaler. Returns null when the session cannot run — callers
 * keep the original image instead of faking quality with bilinear resize.
 */
internal class QualityOnnxUpscaler(private val modelPath: String) {
    fun upscale(rgba: ByteArray, width: Int, height: Int): ProcessedImage? = runCatching {
        com.zakir.vestra.shared.engine.lite.OrtModel(modelPath).use { model ->
            val (inH, inW) = model.inputSize(defaultSize = width.coerceAtMost(512))
            val bitmap = com.zakir.vestra.shared.engine.lite.ImageOps.fromRgba(rgba, width, height)
            val chw = com.zakir.vestra.shared.engine.lite.ImageOps.toNormalizedChw(bitmap, inH, inW)
            val (output, shape) = model.run(chw, inH, inW)
            val outH = shape.getOrNull(2)?.toInt() ?: (height * 2)
            val outW = shape.getOrNull(3)?.toInt() ?: (width * 2)
            val plane = outH * outW
            val outRgba = ByteArray(plane * 4)
            for (i in 0 until plane) {
                val dst = i * 4
                val r = (output[i].coerceIn(0f, 1f) * 255).toInt()
                val g = (output[plane + i].coerceIn(0f, 1f) * 255).toInt()
                val b = (output[2 * plane + i].coerceIn(0f, 1f) * 255).toInt()
                outRgba[dst] = r.toByte()
                outRgba[dst + 1] = g.toByte()
                outRgba[dst + 2] = b.toByte()
                outRgba[dst + 3] = 255.toByte()
            }
            ProcessedImage(outRgba, outW, outH)
        }
    }.getOrNull()
}

/** BiRefNet matte refinement — returns null when ONNX cannot run. */
internal class QualityOnnxMatte(private val modelPath: String) {
    fun refine(rgba: ByteArray, width: Int, height: Int): ByteArray? = runCatching {
        com.zakir.vestra.shared.engine.lite.OrtModel(modelPath).use { model ->
            val (inH, inW) = model.inputSize(defaultSize = 512)
            val bitmap = com.zakir.vestra.shared.engine.lite.ImageOps.fromRgba(rgba, width, height)
            val chw = com.zakir.vestra.shared.engine.lite.ImageOps.toNormalizedChw(bitmap, inH, inW)
            val (output, shape) = model.run(chw, inH, inW)
            val outH = shape.getOrNull(2)?.toInt() ?: inH
            val outW = shape.getOrNull(3)?.toInt() ?: inW
            val plane = outH * outW
            val mask = com.zakir.vestra.shared.engine.lite.ImageOps.normalizeAndResizeMask(
                output.copyOfRange(0, plane),
                outW,
                outH,
                width,
                height,
            )
            val out = rgba.copyOf()
            for (i in mask.indices) {
                val alpha = (mask[i].coerceIn(0f, 1f) * 255).toInt()
                out[i * 4 + 3] = alpha.toByte()
            }
            out
        }
    }.getOrNull()
}
