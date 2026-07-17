package com.zakir.vestra.shared.engine.pro

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import java.nio.FloatBuffer
import java.nio.LongBuffer

/**
 * Thin multi-input/multi-output ONNX Runtime wrapper for the SD1.5 +
 * ControlNet + IP-Adapter graphs, where OrtModel's single-input helper isn't
 * enough. Opens with NNAPI when available (Pro tier is NPU-gated), CPU otherwise.
 */
class OrtGraph(modelPath: String) : AutoCloseable {

    private val env: OrtEnvironment = OrtEnvironment.getEnvironment()
    private val session: OrtSession = env.createSession(
        modelPath,
        OrtSession.SessionOptions().apply { runCatching { addNnapi() } },
    )

    val inputNames: Set<String> get() = session.inputNames.toSet()

    fun floatTensor(data: FloatArray, vararg shape: Long): OnnxTensor =
        OnnxTensor.createTensor(env, FloatBuffer.wrap(data), shape)

    fun longTensor(data: LongArray, vararg shape: Long): OnnxTensor =
        OnnxTensor.createTensor(env, LongBuffer.wrap(data), shape)

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
                    val out = FloatArray(t.info.shape.fold(1L) { a, d -> a * d }.toInt())
                    t.floatBuffer.get(out)
                    out
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
                val out = FloatArray(t.info.shape.fold(1L) { a, d -> a * d }.toInt())
                t.floatBuffer.get(out)
                return out
            }
        } finally {
            inputs.values.forEach { it.close() }
        }
    }

    override fun close() = session.close()
}
