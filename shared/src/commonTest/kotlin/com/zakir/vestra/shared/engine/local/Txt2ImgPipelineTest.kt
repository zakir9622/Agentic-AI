package com.zakir.vestra.shared.engine.local

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class Txt2ImgPipelineTest {

    @Test
    fun sampler_not_wired_in_r2() {
        assertFalse(Txt2ImgPipeline.SAMPLER_WIRED)
    }

    @Test
    fun pipeline_reports_unavailable_until_wired() {
        val pipeline = Txt2ImgPipeline(
            packDir = "/tmp/local-sdturbo-v1",
            config = LocalImagePackConfig(
                graphs = LocalImageGraphs(
                    text_encoder = "text_encoder.onnx",
                    unet = "unet.onnx",
                    vae_decoder = "vae_decoder.onnx",
                ),
            ),
        )
        assertFalse(pipeline.isRunnable())
        val result = pipeline.generate("abaya studio shot", seed = 1L)
        assertTrue(result is LocalImageResult.Unavailable)
        assertTrue(
            (result as LocalImageResult.Unavailable).reason.contains("sampler", ignoreCase = true),
        )
    }
}
