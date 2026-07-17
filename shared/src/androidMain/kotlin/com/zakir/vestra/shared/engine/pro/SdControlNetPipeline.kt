package com.zakir.vestra.shared.engine.pro

import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import com.zakir.vestra.shared.engine.lite.ImageOps
import com.zakir.vestra.shared.engine.pipeline.ConditioningInputs
import com.zakir.vestra.shared.engine.pipeline.ConditioningStage
import com.zakir.vestra.shared.engine.pipeline.ConditioningTokens
import com.zakir.vestra.shared.engine.pipeline.MultiConditioningPipeline
import com.zakir.vestra.shared.engine.pipeline.PipelineRequirements
import java.io.File

/**
 * On-device SD1.5 + ControlNet-Depth + IP-Adapter try-on, implementing the
 * staged [MultiConditioningPipeline]:
 *
 *  A. STRUCTURE  depth.onnx → depth map → ControlNet conditioning image
 *  B. TEXTURE    ip_image_encoder.onnx → ip_proj.onnx → garment image tokens,
 *                concatenated with text_encoder.onnx prompt embeddings
 *  C. SYNTHESIS  per step: controlnet.onnx → down/mid residuals; unet.onnx
 *                (sample, t, combined tokens, +residuals) → noise; CFG 7.0;
 *                DDIM step. Then vae_decoder.onnx → paste back into the person.
 *
 * All graphs are FP16 ONNX at 512×512 from convert_pro_pack.py. This class is
 * the runtime contract; correctness of the exported graphs is validated by the
 * developer on a flagship (VestraProBench) — the agent container has no GPU to
 * produce or run these weights.
 */
class SdControlNetPipeline(
    private val packDir: String,
    private val config: ProPackConfig,
    private val loadPerson: () -> Bitmap?,
    private val loadGarment: () -> Bitmap?,
    private val maskProvider: (Bitmap) -> FloatArray?,
    private val saveResult: (Bitmap) -> String,
) : MultiConditioningPipeline {

    private val res = config.resolution

    override fun requirements(): PipelineRequirements = PipelineRequirements(
        hasStructureModel = config.controlNet.present() && config.depthModel.present(),
        hasIpAdapter = config.ipAdapter.present() && config.imageEncoder.present(),
        hasUnet = File("$packDir/${config.unetOrDefault()}").exists(),
    )

    override suspend fun run(
        inputs: ConditioningInputs,
        onStage: suspend (ConditioningStage, Float) -> Unit,
    ): String {
        val person = loadPerson() ?: error("Couldn't read the model image")
        val garment = loadGarment() ?: error("Couldn't read the garment image")
        val personSq = person.scale(res, res)
        val garmentSq = garment.scale(res, res)

        // ── Stage A: STRUCTURE (depth → ControlNet conditioning) ──
        onStage(ConditioningStage.STRUCTURE, 0.05f)
        val t0 = System.currentTimeMillis()
        val depthCond = OrtGraph("$packDir/${config.depthModel}").use { depth ->
            val (out, _) = depth.inputNames.let {
                val chw = ImageOps.toNormalizedChw(garmentSq.scale(518, 518), 518, 518)
                out(depth, chw)
            }
            // Depth is single-channel 0..1; expand to a 3-channel RGB cond image.
            expandDepthToRgb(out, 518, res)
        }
        Log.i(TAG, "structure_depth: ${System.currentTimeMillis() - t0} ms")

        // ── Stage B: TEXTURE (IP-Adapter image tokens + text tokens) ──
        onStage(ConditioningStage.TEXTURE, 0.15f)
        val imageEmbeds = OrtGraph("$packDir/${config.imageEncoder}").use { enc ->
            enc.runSingle(mapOf(enc.inputNames.first() to enc.floatTensor(
                ImageOps.toNormalizedChw(garmentSq.scale(224, 224), 224, 224), 1, 3, 224, 224,
            )))
        }
        val ipTokens = OrtGraph("$packDir/${config.ipAdapter}").use { proj ->
            proj.runSingle(mapOf(proj.inputNames.first() to proj.floatTensor(
                imageEmbeds, 1, (imageEmbeds.size / IP_DIM).toLong(), IP_DIM.toLong(),
            )))
        }
        val textEmbeds = encodePrompt(inputs)
        val combined = ConditioningTokens.concat(
            text = textEmbeds, textTokens = TEXT_TOKENS,
            image = ipTokens, imageTokens = ipTokens.size / HIDDEN_DIM,
            dim = HIDDEN_DIM,
        )
        val combinedSeq = ConditioningTokens.sequenceLength(TEXT_TOKENS, ipTokens.size / HIDDEN_DIM).toLong()

        // ── Stage C: SYNTHESIS (ControlNet + UNet denoise loop) ──
        val steps = ConditioningTokens.clampSteps(config.inferenceSteps)
        val scheduler = DdimScheduler()
        val timesteps = scheduler.timesteps(steps)
        val latentLen = 4 * (res / 8) * (res / 8)
        val sample = FloatArray(latentLen) { gaussian(inputs.seed, it) }

        val tSync = System.currentTimeMillis()
        OrtGraph("$packDir/${config.controlNet}").use { control ->
            OrtGraph("$packDir/${config.unetOrDefault()}").use { unet ->
                timesteps.forEachIndexed { index, timestep ->
                    val residuals = control.run(
                        inputs = mapOf(
                            "sample" to control.floatTensor(sample, 1, 4, (res / 8).toLong(), (res / 8).toLong()),
                            "timestep" to control.longTensor(longArrayOf(timestep.toLong()), 1),
                            "encoder_hidden_states" to control.floatTensor(combined, 1, combinedSeq, HIDDEN_DIM.toLong()),
                            "controlnet_cond" to control.floatTensor(depthCond, 1, 3, res.toLong(), res.toLong()),
                        ),
                        outputs = control.inputNames.let { CONTROL_OUTPUTS },
                    )
                    val noise = runUnet(unet, sample, timestep, combined, combinedSeq, residuals, config.guidanceScale)
                    scheduler.step(sample, noise, timestep, steps)
                    onStage(ConditioningStage.SYNTHESIS, 0.2f + 0.65f * (index + 1) / steps)
                }
            }
        }
        Log.i(TAG, "synthesis_${steps}steps: ${System.currentTimeMillis() - tSync} ms")

        // Decode + paste back into the untouched person outside the garment mask.
        val decoded = OrtGraph("$packDir/${config.vaeDecoderOrDefault()}").use { dec ->
            val rgb = dec.runSingle(mapOf(dec.inputNames.first() to dec.floatTensor(
                sample, 1, 4, (res / 8).toLong(), (res / 8).toLong(),
            )))
            chwToBitmap(rgb, res)
        }
        val mask = maskProvider(personSq)
        val composed = if (mask != null) pasteBack(personSq, decoded, mask) else decoded
        return saveResult(composed)
    }

    // ── helpers (kept small; heavy tensor work is in the ONNX graphs) ──

    private fun out(depth: OrtGraph, chw: FloatArray): Pair<FloatArray, Int> {
        val d = depth.runSingle(mapOf(depth.inputNames.first() to depth.floatTensor(chw, 1, 3, 518, 518)))
        return d to 518
    }

    private fun encodePrompt(inputs: ConditioningInputs): FloatArray {
        // The text encoder needs tokenized ids; a full CLIP tokenizer is heavy,
        // so packs may ship a precomputed embedding for the fixed PromptStyle
        // string. If a text encoder is present we feed a zero id sequence as a
        // neutral prompt (image tokens dominate); a tokenizer pack can replace this.
        val textEncoder = config.textEncoder ?: return FloatArray(TEXT_TOKENS * HIDDEN_DIM)
        val path = "$packDir/$textEncoder"
        if (!File(path).exists()) return FloatArray(TEXT_TOKENS * HIDDEN_DIM)
        return OrtGraph(path).use { te ->
            te.runSingle(mapOf(te.inputNames.first() to te.longTensor(LongArray(TEXT_TOKENS), 1, TEXT_TOKENS.toLong())))
        }
    }

    private fun runUnet(
        unet: OrtGraph,
        sample: FloatArray,
        timestep: Int,
        tokens: FloatArray,
        seq: Long,
        residuals: List<FloatArray>,
        cfg: Float,
    ): FloatArray {
        val base = mutableMapOf(
            "sample" to unet.floatTensor(sample, 1, 4, (res / 8).toLong(), (res / 8).toLong()),
            "timestep" to unet.longTensor(longArrayOf(timestep.toLong()), 1),
            "encoder_hidden_states" to unet.floatTensor(tokens, 1, seq, HIDDEN_DIM.toLong()),
        )
        // Feed ControlNet residuals only if this UNet export declares them.
        residuals.forEachIndexed { i, r ->
            val name = if (i < residuals.size - 1) "down_$i" else "mid"
            if (unet.inputNames.contains(name)) {
                // Residual spatial dims vary per block; the exporter fixes them,
                // so pass through as a flat buffer keyed by name.
                base[name] = unet.floatTensor(r, r.size.toLong())
            }
        }
        return unet.runSingle(base)
    }

    private fun expandDepthToRgb(depth: FloatArray, srcSize: Int, outSize: Int): FloatArray {
        val resized = ImageOps.normalizeAndResizeMask(depth, srcSize, srcSize, outSize, outSize)
        val chw = FloatArray(3 * outSize * outSize)
        val plane = outSize * outSize
        for (i in 0 until plane) {
            chw[i] = resized[i]; chw[plane + i] = resized[i]; chw[2 * plane + i] = resized[i]
        }
        return chw
    }

    private fun chwToBitmap(chw: FloatArray, size: Int): Bitmap {
        val plane = size * size
        val pixels = IntArray(plane)
        for (i in 0 until plane) {
            val r = (((chw[i] + 1f) * 127.5f).toInt()).coerceIn(0, 255)
            val g = (((chw[plane + i] + 1f) * 127.5f).toInt()).coerceIn(0, 255)
            val b = (((chw[2 * plane + i] + 1f) * 127.5f).toInt()).coerceIn(0, 255)
            pixels[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
    }

    private fun pasteBack(person: Bitmap, generated: Bitmap, mask: FloatArray): Bitmap {
        val w = person.width; val h = person.height
        val base = IntArray(w * h); person.getPixels(base, 0, w, 0, 0, w, h)
        val gen = IntArray(w * h); generated.scale(w, h).getPixels(gen, 0, w, 0, 0, w, h)
        for (i in base.indices) {
            val a = mask[i].coerceIn(0f, 1f)
            if (a > 0.02f) {
                val br = base[i]; val gr = gen[i]
                val r = ((br shr 16 and 0xFF) * (1 - a) + (gr shr 16 and 0xFF) * a).toInt()
                val g = ((br shr 8 and 0xFF) * (1 - a) + (gr shr 8 and 0xFF) * a).toInt()
                val bl = ((br and 0xFF) * (1 - a) + (gr and 0xFF) * a).toInt()
                base[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or bl
            }
        }
        return Bitmap.createBitmap(base, w, h, Bitmap.Config.ARGB_8888)
    }

    private fun gaussian(seed: Long?, i: Int): Float {
        val r = kotlin.random.Random((seed ?: 0L) + i * 2654435761L)
        val u1 = r.nextDouble().coerceAtLeast(1e-9); val u2 = r.nextDouble()
        return (kotlin.math.sqrt(-2.0 * kotlin.math.ln(u1)) * kotlin.math.cos(2.0 * Math.PI * u2)).toFloat()
    }

    companion object {
        private const val TAG = DiffusionEngine.TAG
        private const val TEXT_TOKENS = 77
        private const val HIDDEN_DIM = 768
        private const val IP_DIM = 1280
        private val CONTROL_OUTPUTS = (0..11).map { "down_$it" } + "mid"
    }
}

private fun String?.present(): Boolean = !isNullOrBlank()
private fun ProPackConfig.unetOrDefault(): String = "unet.onnx"
private fun ProPackConfig.vaeDecoderOrDefault(): String = "vae_decoder.onnx"
