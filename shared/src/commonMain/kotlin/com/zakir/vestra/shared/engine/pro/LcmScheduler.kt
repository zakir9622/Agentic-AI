package com.zakir.vestra.shared.engine.pro

import kotlin.math.sqrt

/**
 * LCM (Latent Consistency Model) scheduler for distilled tiny-SD packs.
 * Boundary-condition step aligned with diffusers LCMScheduler (sigma_data=0.5, scaling=10).
 */
class LcmScheduler(
    private val trainSteps: Int = 1000,
    private val sigmaData: Float = 0.5f,
    private val timestepScaling: Float = 10f,
) {
    private val ddim = DdimScheduler(trainSteps)

    /** Descending timesteps from T-1 toward 0. */
    fun timesteps(inferenceSteps: Int): IntArray {
        if (inferenceSteps <= 1) return intArrayOf(trainSteps - 1)
        return IntArray(inferenceSteps) { i ->
            (trainSteps - 1) * (inferenceSteps - 1 - i) / (inferenceSteps - 1)
        }
    }

    /** One LCM reverse step — updates [sample] in place. */
    fun step(sample: FloatArray, noisePred: FloatArray, timestep: Int) {
        val scaledT = timestep.toFloat() * timestepScaling / trainSteps
        val cSkip = (sigmaData * sigmaData) / (scaledT * scaledT + sigmaData * sigmaData)
        val cOut = scaledT / sqrt(scaledT * scaledT + sigmaData * sigmaData)
        for (i in sample.indices) {
            sample[i] = cOut * noisePred[i] + cSkip * sample[i]
        }
    }

    /** Forward-noise a clean latent (img2img init). */
    fun addNoise(clean: FloatArray, noise: FloatArray, timestep: Int): FloatArray =
        ddim.addNoise(clean, noise, timestep)
}
