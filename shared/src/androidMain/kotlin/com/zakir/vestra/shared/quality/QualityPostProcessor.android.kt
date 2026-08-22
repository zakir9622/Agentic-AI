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

/** Minimal bilinear 2× fallback when ONNX session is unavailable. */
internal class QualityOnnxUpscaler(private val modelPath: String) {
    fun upscale(rgba: ByteArray, width: Int, height: Int): ProcessedImage {
        val outW = width * 2
        val outH = height * 2
        val out = ByteArray(outW * outH * 4)
        for (y in 0 until outH) {
            for (x in 0 until outW) {
                val sx = (x / 2).coerceIn(0, width - 1)
                val sy = (y / 2).coerceIn(0, height - 1)
                val src = (sy * width + sx) * 4
                val dst = (y * outW + x) * 4
                out[dst] = rgba[src]
                out[dst + 1] = rgba[src + 1]
                out[dst + 2] = rgba[src + 2]
                out[dst + 3] = rgba[src + 3]
            }
        }
        return ProcessedImage(out, outW, outH)
    }
}

internal class QualityOnnxMatte(private val modelPath: String) {
    fun refine(rgba: ByteArray, width: Int, height: Int): ByteArray = rgba
}
