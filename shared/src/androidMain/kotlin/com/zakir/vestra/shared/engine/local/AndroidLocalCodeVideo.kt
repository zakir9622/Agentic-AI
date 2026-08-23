package com.zakir.vestra.shared.engine.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaMuxer
import android.os.Build
import android.view.Surface
import com.zakir.vestra.shared.packs.ModelPackManager
import java.io.File
import java.nio.ByteBuffer

/**
 * Offline Video Studio — short MP4 still-clip from [LocalImageGenerator].
 * True diffusion video is not phone-practical; this still produces playable local output.
 */
class AndroidLocalVideoGenerator(
    private val localImage: LocalImageGenerator,
    private val outputDir: File,
) : LocalVideoGenerator {

    override fun isReady(): Boolean = localImage.isReady()

    override fun generate(prompt: String, seed: Long?): LocalVideoResult {
        if (!isReady()) {
            return LocalVideoResult.Unavailable(
                "Install local-sdturbo-v1 from Model packs for offline video still-clips.",
            )
        }
        return when (val image = localImage.generate(prompt, seed, referenceImageUri = null)) {
            is LocalImageResult.Unavailable -> LocalVideoResult.Unavailable(image.reason)
            is LocalImageResult.Ok -> runCatching {
                val bitmap = BitmapFactory.decodeFile(image.imagePath)
                    ?: error("Could not decode local image for video")
                outputDir.mkdirs()
                val out = File(outputDir, "local_vid_${System.currentTimeMillis()}.mp4")
                encodeStillMp4(bitmap, out, durationMs = 2_500, fps = 8)
                bitmap.recycle()
                LocalVideoResult.Ok(out.absolutePath)
            }.getOrElse { err ->
                LocalVideoResult.Unavailable(
                    err.message?.take(160) ?: "Local still-clip encode failed",
                )
            }
        }
    }

    companion object {
        /** Encode a single bitmap as a short H.264 MP4 (repeated frames via Surface input). */
        fun encodeStillMp4(bitmap: Bitmap, outFile: File, durationMs: Int, fps: Int) {
            val width = (bitmap.width / 2 * 2).coerceAtLeast(2)
            val height = (bitmap.height / 2 * 2).coerceAtLeast(2)
            val scaled = if (bitmap.width == width && bitmap.height == height) {
                bitmap
            } else {
                Bitmap.createScaledBitmap(bitmap, width, height, true)
            }
            val frameCount = (durationMs * fps / 1000).coerceAtLeast(1)
            val mime = "video/avc"
            val format = MediaFormat.createVideoFormat(mime, width, height).apply {
                setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface)
                setInteger(MediaFormat.KEY_BIT_RATE, 1_500_000)
                setInteger(MediaFormat.KEY_FRAME_RATE, fps)
                setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 1)
            }
            val codec = MediaCodec.createEncoderByType(mime)
            var inputSurface: Surface? = null
            val muxer = MediaMuxer(outFile.absolutePath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
            var track = -1
            var muxerStarted = false
            val bufferInfo = MediaCodec.BufferInfo()
            try {
                codec.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                inputSurface = codec.createInputSurface()
                codec.start()
                var frame = 0
                var inputDone = false
                var outputDone = false
                while (!outputDone) {
                    if (!inputDone) {
                        if (frame < frameCount) {
                            val canvas = if (Build.VERSION.SDK_INT >= 23) {
                                inputSurface!!.lockHardwareCanvas()
                            } else {
                                @Suppress("DEPRECATION")
                                inputSurface!!.lockCanvas(null)
                            }
                            try {
                                canvas.drawBitmap(scaled, 0f, 0f, null)
                            } finally {
                                inputSurface.unlockCanvasAndPost(canvas)
                            }
                            frame++
                        } else {
                            codec.signalEndOfInputStream()
                            inputDone = true
                        }
                    }
                    when (val outIndex = codec.dequeueOutputBuffer(bufferInfo, 50_000L)) {
                        MediaCodec.INFO_TRY_AGAIN_LATER -> Unit
                        MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                            if (muxerStarted) error("format changed twice")
                            track = muxer.addTrack(codec.outputFormat)
                            muxer.start()
                            muxerStarted = true
                        }
                        else -> if (outIndex >= 0) {
                            val outBuf: ByteBuffer = codec.getOutputBuffer(outIndex)
                                ?: error("null output buffer")
                            if (bufferInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG != 0) {
                                bufferInfo.size = 0
                            }
                            if (bufferInfo.size > 0 && muxerStarted) {
                                outBuf.position(bufferInfo.offset)
                                outBuf.limit(bufferInfo.offset + bufferInfo.size)
                                muxer.writeSampleData(track, outBuf, bufferInfo)
                            }
                            outputDone = bufferInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                            codec.releaseOutputBuffer(outIndex, false)
                        }
                    }
                }
            } finally {
                runCatching { codec.stop() }
                runCatching { codec.release() }
                runCatching { inputSurface?.release() }
                if (muxerStarted) runCatching { muxer.stop() }
                runCatching { muxer.release() }
                if (scaled !== bitmap) scaled.recycle()
            }
        }
    }
}

/**
 * Offline Code Studio via MediaPipe LLM Inference + Gemma 3 1B INT4 pack.
 */
class AndroidLocalCodeGenerator(
    private val context: Context,
    private val packs: ModelPackManager,
    private val packId: String = PACK_ID,
) : LocalCodeGenerator {

    override fun isReady(): Boolean {
        if (!packs.isReady(packId)) return false
        val dir = packs.installedDir(packId) ?: return false
        val task = File(dir, TASK_FILE)
        return task.isFile && task.length() > MIN_TASK_BYTES
    }

    override fun generate(prompt: String, system: String): LocalCodeResult {
        if (!isReady()) {
            return LocalCodeResult.Unavailable(
                "Download local-gemma-v1 (~530 MB) from Model packs for offline Code Studio.",
            )
        }
        val dir = packs.installedDir(packId)
            ?: return LocalCodeResult.Unavailable("Gemma pack directory missing.")
        val modelPath = File(dir, TASK_FILE).absolutePath
        return runCatching {
            generateWithMediaPipe(modelPath, prompt, system)
        }.getOrElse { err ->
            LocalCodeResult.Unavailable(
                err.message?.take(200)
                    ?: "On-device Gemma failed — re-download local-gemma-v1 or use cloud Code.",
            )
        }
    }

    private fun generateWithMediaPipe(modelPath: String, prompt: String, system: String): LocalCodeResult {
        val llmClass = Class.forName("com.google.mediapipe.tasks.genai.llminference.LlmInference")
        val optionsClass = Class.forName(
            "com.google.mediapipe.tasks.genai.llminference.LlmInference\$LlmInferenceOptions",
        )
        val builder = optionsClass.getMethod("builder").invoke(null)
        builder.javaClass.getMethod("setModelPath", String::class.java).invoke(builder, modelPath)
        builder.javaClass.getMethod("setMaxTokens", Int::class.javaPrimitiveType).invoke(builder, 1024)
        val options = builder.javaClass.getMethod("build").invoke(builder)
        val create = llmClass.getMethod("createFromOptions", Context::class.java, optionsClass)
        val llm = create.invoke(null, context.applicationContext, options)
        try {
            val fullPrompt = buildString {
                if (system.isNotBlank()) {
                    append(system.trim())
                    append("\n\n")
                }
                append(prompt.trim())
            }
            val text = llmClass.getMethod("generateResponse", String::class.java)
                .invoke(llm, fullPrompt) as String
            if (text.isBlank()) {
                return LocalCodeResult.Unavailable("Gemma returned empty text.")
            }
            return LocalCodeResult.Ok(text.trim(), tokensIn = fullPrompt.length / 4, tokensOut = text.length / 4)
        } finally {
            runCatching { llmClass.getMethod("close").invoke(llm) }
        }
    }

    companion object {
        const val PACK_ID = "local-gemma-v1"
        const val TASK_FILE = "gemma3-1b-it-int4.task"
        const val MIN_TASK_BYTES = 50_000_000L
    }
}
