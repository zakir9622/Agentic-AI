package com.zakir.vestra.shared.engine.pro

import ai.onnxruntime.OnnxJavaType
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import com.zakir.vestra.shared.engine.lite.OrtModel
import java.nio.FloatBuffer
import java.nio.IntBuffer
import java.nio.LongBuffer

/**
 * Thin multi-input/multi-output ONNX Runtime wrapper for the SD1.5 +
 * ControlNet + IP-Adapter graphs (and local txt2img). Soft-wraps native link
 * failures like [OrtModel]; caps output element counts to avoid OOM.
 */
class OrtGraph(modelPath: String) : AutoCloseable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = createSessionSafely(modelPath)

    val inputNames: Set<String> get() = session.inputNames.toSet()

    fun floatTensor(data: FloatArray, vararg shape: Long): OnnxTensor =
        OnnxTensor.createTensor(env, FloatBuffer.wrap(data), shape)

    fun longTensor(data: LongArray, vararg shape: Long): OnnxTensor =
        OnnxTensor.createTensor(env, LongBuffer.wrap(data), shape)

    fun intTensor(data: IntArray, vararg shape: Long): OnnxTensor =
        OnnxTensor.createTensor(env, IntBuffer.wrap(data), shape)

    /**
     * ONNX element type an input declares, or null when the graph doesn't say.
     *
     * Text-encoder exports disagree about token id width: diffusers exports `input_ids` as
     * int32, others as int64. Hardcoding int64 made every local image generation fail with
     * `ORT_INVALID_ARGUMENT ... Actual: (tensor(int64)), expected: (tensor(int32))`, so token
     * tensors are built to match whatever the loaded graph actually declares.
     */
    fun inputType(name: String): OnnxJavaType? = runCatching {
        (session.inputInfo[name]?.info as? TensorInfo)?.type
    }.getOrNull()

    /** Token-id tensor typed to match [inputName]; falls back to int32, the common export. */
    fun tokenTensor(inputName: String, ids: LongArray, vararg shape: Long): OnnxTensor =
        when (inputType(inputName)) {
            OnnxJavaType.INT64 -> longTensor(ids, *shape)
            else -> intTensor(IntArray(ids.size) { ids[it].toInt() }, *shape)
        }

    /**
     * Runs the graph and returns each requested output flattened, in order.
     * Input tensors are closed after the run — callers pass freshly-created
     * tensors per invocation (e.g. one per denoise step), so leaving them open
     * would leak native buffers across the loop.
     */
    fun run(inputs: Map<String, OnnxTensor>, outputs: List<String>): List<FloatArray> {
        try {
            session.run(inputs).use { result ->
                return outputs.map { name ->
                    val t = result.get(name).get() as OnnxTensor
                    readFloats(t)
                }
            }
        } finally {
            inputs.values.forEach { it.close() }
        }
    }

    /** Convenience for a single named output; closes input tensors afterward. */
    fun runSingle(inputs: Map<String, OnnxTensor>): FloatArray {
        try {
            session.run(inputs).use { result ->
                val t = result[0] as OnnxTensor
                return readFloats(t)
            }
        } finally {
            inputs.values.forEach { it.close() }
        }
    }

    override fun close() {
        runCatching { session.close() }
    }

    companion object {
        private fun readFloats(t: OnnxTensor): FloatArray {
            val count = OrtModel.elementCount(t.info.shape)
            require(count in 1..OrtModel.MAX_OUTPUT_ELEMENTS) {
                "ONNX output size $count outside safe range (max ${OrtModel.MAX_OUTPUT_ELEMENTS})"
            }
            val out = FloatArray(count)
            t.floatBuffer.get(out)
            return out
        }

        private fun createSessionSafely(modelPath: String): OrtSession =
            // Pro FP16 packs: NO_OPT + no QNN (see ProOrtSessions). Soft-wraps link/load errors.
            ProOrtSessions.create(modelPath)
    }
}
