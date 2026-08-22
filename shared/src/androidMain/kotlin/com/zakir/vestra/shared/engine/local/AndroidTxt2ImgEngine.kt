package com.zakir.vestra.shared.engine.local

import ai.onnxruntime.OnnxTensor
import android.graphics.Bitmap
import com.zakir.vestra.shared.engine.pro.ClipTokenizer
import com.zakir.vestra.shared.engine.pro.DdimScheduler
import com.zakir.vestra.shared.engine.pro.DiffusionSteps
import com.zakir.vestra.shared.engine.pro.OrtGraph
import java.io.File
import java.io.FileOutputStream
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * True on-device SD-Turbo / LCM txt2img (4-channel UNet — not Pro 9-ch inpaint).
 *
 * Requires pack files:
 * - text_encoder.onnx, unet.onnx, vae_decoder.onnx (≥ 1 MB each)
 * - vocab.json + merges.txt (CLIP tokenizer)
 */
class AndroidTxt2ImgEngine(
    private val packDir: File,
    private val config: LocalImagePackConfig,
) : AutoCloseable {

    private val resolution = config.resolution.coerceIn(256, 768)
    private val latent = resolution / 8
    private val plane = latent * latent
    private val vaeScale = 0.18215f

    private val textEncoderName = config.graphs?.textEncoder ?: "text_encoder.onnx"
    private val unetName = config.graphs?.unet ?: "unet.onnx"
    private val vaeName = config.graphs?.vaeDecoder ?: "vae_decoder.onnx"

    private val textEncoder = OrtGraph(File(packDir, textEncoderName).absolutePath)
    private val unet = OrtGraph(File(packDir, unetName).absolutePath)
    private val vaeDecoder = OrtGraph(File(packDir, vaeName).absolutePath)
    private val tokenizer = runCatching { ClipTokenizer(packDir.absolutePath) }.getOrNull()

    fun generate(prompt: String, seed: Long?, outputDir: File): LocalImageResult {
        val trimmed = prompt.trim()
        if (trimmed.isEmpty()) {
            return LocalImageResult.Unavailable("Prompt is empty")
        }
        if (tokenizer == null) {
            return LocalImageResult.Unavailable(
                "Pack missing vocab.json / merges.txt — re-export local-sdturbo-v1 with CLIP tokenizer files.",
            )
        }
        return runCatching {
            val steps = DiffusionSteps.resolve(
                inferenceSteps = config.scheduler?.steps ?: 4,
                lcmDistilled = config.lcmDistilled,
            ).coerceIn(1, 12)
            val guidance = config.scheduler?.guidance ?: 1.0f
            val rng = Random(seed ?: System.currentTimeMillis())

            val cond = encodeText(trimmed)
            val uncond = if (guidance > 1.01f) encodeText("") else null

            val sample = FloatArray(4 * plane) { gaussian(rng) }
            val scheduler = DdimScheduler()
            val timesteps = scheduler.timesteps(steps)

            for (t in timesteps) {
                val noiseCond = predictNoise(sample, cond, t)
                val noise = if (uncond != null && guidance > 1.01f) {
                    val noiseUncond = predictNoise(sample, uncond, t)
                    FloatArray(noiseCond.size) { i ->
                        noiseUncond[i] + guidance * (noiseCond[i] - noiseUncond[i])
                    }
                } else {
                    noiseCond
                }
                scheduler.step(sample, noise, t, steps)
            }

            val rgb = decodeVae(sample)
            val bitmap = fromChw(rgb, resolution, resolution)
            outputDir.mkdirs()
            val out = File(outputDir, "local_img_${System.currentTimeMillis()}.png")
            FileOutputStream(out).use { fos ->
                if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)) {
                    error("Failed to encode PNG")
                }
            }
            bitmap.recycle()
            LocalImageResult.Ok(out.absolutePath)
        }.getOrElse { err ->
            LocalImageResult.Unavailable(
                err.message?.take(200) ?: "On-device image generation failed",
            )
        }
    }

    private fun encodeText(text: String): FloatArray {
        val ids = tokenizer!!.encode(text, maxLength = 77)
        val inputName = textEncoder.inputNames.firstOrNull {
            it.contains("input", ignoreCase = true) || it.contains("ids", ignoreCase = true)
        } ?: textEncoder.inputNames.first()
        val tensor = textEncoder.longTensor(ids, 1, 77)
        return textEncoder.runSingle(mapOf(inputName to tensor))
    }

    private fun predictNoise(sample: FloatArray, hidden: FloatArray, timestep: Int): FloatArray {
        val names = unet.inputNames.toList()
        val sampleName = names.firstOrNull { it.contains("sample", true) } ?: names[0]
        val timeName = names.firstOrNull { it.contains("time", true) }
            ?: names.getOrNull(1)
            ?: names[0]
        val hiddenName = names.firstOrNull {
            it.contains("hidden", true) || it.contains("encoder", true)
        }

        val sampleTensor = unet.floatTensor(sample, 1, 4, latent.toLong(), latent.toLong())
        val timeTensor = OnnxTensor.createTensor(
            ai.onnxruntime.OrtEnvironment.getEnvironment(),
            longArrayOf(timestep.toLong()),
        )
        val hiddenTensor = hiddenName?.let {
            val seq = if (hidden.size % 768 == 0) hidden.size / 768 else 77
            val dim = if (hidden.size % 768 == 0) 768 else (hidden.size / seq).coerceAtLeast(1)
            val data = if (hidden.size == seq * dim) {
                hidden
            } else {
                FloatArray(seq * dim).also { dst ->
                    hidden.copyInto(dst, endIndex = minOf(hidden.size, dst.size))
                }
            }
            unet.floatTensor(data, 1, seq.toLong(), dim.toLong())
        }

        val inputs = buildMap {
            put(sampleName, sampleTensor)
            put(timeName, timeTensor)
            if (hiddenName != null && hiddenTensor != null) put(hiddenName, hiddenTensor)
        }
        return unet.runSingle(inputs)
    }

    private fun decodeVae(latentSample: FloatArray): FloatArray {
        val scaled = FloatArray(latentSample.size) { i -> latentSample[i] / vaeScale }
        val name = vaeDecoder.inputNames.first()
        val tensor = vaeDecoder.floatTensor(scaled, 1, 4, latent.toLong(), latent.toLong())
        return vaeDecoder.runSingle(mapOf(name to tensor))
    }

    private fun fromChw(chw: FloatArray, width: Int, height: Int): Bitmap {
        val planeSize = width * height
        require(chw.size >= 3 * planeSize) { "VAE output too small for ${width}x$height" }
        val pixels = IntArray(planeSize)
        for (i in 0 until planeSize) {
            val r = ((chw[i] + 1f) * 127.5f).roundToInt().coerceIn(0, 255)
            val g = ((chw[planeSize + i] + 1f) * 127.5f).roundToInt().coerceIn(0, 255)
            val b = ((chw[2 * planeSize + i] + 1f) * 127.5f).roundToInt().coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)
    }

    private fun gaussian(random: Random): Float {
        var u = 0f
        var v = 0f
        while (u <= Float.MIN_VALUE) {
            u = random.nextFloat()
            v = random.nextFloat()
        }
        return sqrt(-2f * ln(u)) * cos((2.0 * Math.PI * v).toFloat())
    }

    override fun close() {
        runCatching { textEncoder.close() }
        runCatching { unet.close() }
        runCatching { vaeDecoder.close() }
    }
}
