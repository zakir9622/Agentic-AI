package com.zakir.vestra.shared.engine.lite

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import java.nio.FloatBuffer

/**
 * Thin wrapper around an ONNX Runtime session for single-input image models.
 * Input geometry is read from the model itself so exports can change size
 * without touching app code.
 */
class OrtModel(modelPath: String) : AutoCloseable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(
        modelPath,
        OrtSession.SessionOptions().apply {
            // NNAPI is attempted opportunistically; CPU is the guaranteed path.
            runCatching { addNnapi() }
        },
    )

    private val inputName: String = session.inputNames.firstOrNull()
        ?: error("ONNX model has no inputs: $modelPath")

    /** [height, width] the model expects; dynamic dims fall back to [defaultSize]. */
    fun inputSize(defaultSize: Int = 320): Pair<Int, Int> {
        val info = session.inputInfo[inputName]?.info
        val shape = (info as? TensorInfo)?.shape ?: return defaultSize to defaultSize
        // NCHW: [batch, channels, height, width]
        val h = shape.getOrNull(2)?.toInt()?.takeIf { it > 0 } ?: defaultSize
        val w = shape.getOrNull(3)?.toInt()?.takeIf { it > 0 } ?: defaultSize
        return h to w
    }

    /**
     * Runs the model on an NCHW float tensor and returns the first output as
     * (flatData, shape).
     */
    fun run(chw: FloatArray, height: Int, width: Int): Pair<FloatArray, LongArray> {
        val tensor = OnnxTensor.createTensor(
            env,
            FloatBuffer.wrap(chw),
            longArrayOf(1, 3, height.toLong(), width.toLong()),
        )
        tensor.use {
            session.run(mapOf(inputName to tensor)).use { results ->
                val output = results[0] as? OnnxTensor
                    ?: error("ONNX output 0 is not a tensor")
                val shape = output.info.shape
                val count = elementCount(shape)
                require(count in 1..MAX_OUTPUT_ELEMENTS) {
                    "ONNX output size $count outside safe range (shape=${shape.contentToString()})"
                }
                val data = FloatArray(count)
                output.floatBuffer.get(data)
                return data to shape
            }
        }
    }

    override fun close() {
        session.close()
    }

    companion object {
        /** ~256 MB float32 ceiling — refuse pathological / dynamic-dim explosions. */
        const val MAX_OUTPUT_ELEMENTS = 64 * 1024 * 1024

        internal fun elementCount(shape: LongArray): Int {
            var acc = 1L
            for (d in shape) {
                if (d <= 0L) return -1
                if (acc > Long.MAX_VALUE / d) return -1
                acc *= d
                if (acc > Int.MAX_VALUE) return -1
            }
            return acc.toInt()
        }
    }
}
