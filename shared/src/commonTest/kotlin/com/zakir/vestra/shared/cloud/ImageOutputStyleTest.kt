package com.zakir.vestra.shared.cloud

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The value of this resolver is entirely in what it *declines* to do.
 *
 * Making portrait-and-photoreal the default is trivial; the part that can quietly ruin output is
 * applying it to a prompt that asked for something else. These cases pin that boundary.
 */
class ImageOutputStyleTest {

    @Test
    fun defaultsToPortraitRatherThanSquare() {
        val spec = ImageOutputStyle.resolve("a woman in a red coat")
        assertEquals(832, spec.width)
        assertEquals(1216, spec.height)
        assertTrue(spec.height > spec.width, "the default frame must be taller than it is wide")
    }

    @Test
    fun portraitSpendsTheSamePixelBudgetAsTheSquareItReplaced() {
        // Free ZeroGPU Spaces are budgeted in queue seconds, which track pixel count. A portrait
        // frame that quietly doubled the area would turn successful generations into timeouts.
        val portrait = ImageOutputStyle.PORTRAIT.first * ImageOutputStyle.PORTRAIT.second
        val square = ImageOutputStyle.SQUARE.first * ImageOutputStyle.SQUARE.second
        val ratio = portrait.toDouble() / square.toDouble()
        assertTrue(ratio in 0.9..1.1, "portrait is $ratio x the old square's pixel budget")
    }

    @Test
    fun addsThePhotorealFinishByDefault() {
        val out = ImageOutputStyle.styledPrompt("a woman in a red coat")
        assertTrue(out.startsWith("a woman in a red coat"), "the user's own words must come first")
        assertTrue(out.contains("ultra realistic"), out)
    }

    @Test
    fun withholdsThePhotorealFinishWhenThePromptIsNotAPhotograph() {
        // The failure this prevents: "anime portrait of a knight. ultra realistic, photographic"
        // hands the model two contradictory instructions.
        for (prompt in listOf(
            "anime portrait of a knight",
            "flat vector logo for a coffee shop",
            "watercolor painting of a harbour",
            "pixel art spaceship",
            "3d render of a chair",
            "pencil sketch of a cat",
        )) {
            assertNull(
                ImageOutputStyle.resolve(prompt).styleSuffix,
                "photoreal finish must not be forced onto: $prompt",
            )
            assertEquals(prompt, ImageOutputStyle.styledPrompt(prompt))
        }
    }

    @Test
    fun doesNotRepeatRealismVocabularyThePromptAlreadyHas() {
        val prompt = "ultra realistic portrait of an old fisherman"
        assertEquals(prompt, ImageOutputStyle.styledPrompt(prompt))
    }

    @Test
    fun honoursAnExplicitLandscapeRequest() {
        for (prompt in listOf(
            "a wide shot of a mountain range",
            "panoramic city skyline",
            "16:9 cinematic shot of a desert",
            "desktop wallpaper of a forest",
        )) {
            val spec = ImageOutputStyle.resolve(prompt)
            assertTrue(spec.width > spec.height, "expected a wide frame for: $prompt")
        }
    }

    @Test
    fun honoursAnExplicitSquareRequest() {
        for (prompt in listOf("album cover for a jazz record", "square profile picture", "1:1 sticker")) {
            val spec = ImageOutputStyle.resolve(prompt)
            assertEquals(spec.width, spec.height, "expected a square frame for: $prompt")
        }
    }

    @Test
    fun landscapeStillGetsThePhotorealFinishWhenItIsAPhotograph() {
        // Frame and finish are independent decisions; asking for a wide shot says nothing about
        // whether it should look like a photograph.
        val spec = ImageOutputStyle.resolve("a wide shot of a mountain range")
        assertNotNull(spec.styleSuffix)
        assertTrue(spec.width > spec.height)
    }

    @Test
    fun anIllustratedLandscapeGetsTheWideFrameAndNoPhotorealFinish() {
        // "panoramic", not a bare "wide": the detector deliberately matches "wide shot" and
        // "wide angle" but not "wide" alone, because "wide-brimmed hat" and "wide eyes" are
        // descriptions of a subject, not a request for a frame. This case is about frame and
        // finish being decided independently, so it uses a word that only means one thing.
        val spec = ImageOutputStyle.resolve("panoramic watercolor painting of a harbour")
        assertTrue(spec.width > spec.height)
        assertNull(spec.styleSuffix)
    }

    @Test
    fun aBareWideDoesNotHijackTheFrame() {
        // Guards the deliberate omission above: these describe a subject, not a frame.
        for (prompt in listOf("a woman in a wide-brimmed hat", "close up of wide eyes")) {
            val spec = ImageOutputStyle.resolve(prompt)
            assertTrue(spec.height > spec.width, "expected portrait for: $prompt")
        }
    }

    @Test
    fun preferPhotorealFalseTurnsTheFinishOffEntirely() {
        assertNull(ImageOutputStyle.resolve("a woman in a red coat", preferPhotoreal = false).styleSuffix)
    }

    @Test
    fun theFluxPayloadCarriesTheResolvedFrameRatherThanAHardcodedSquare() {
        // Guards the wiring, not just the resolver: the payload used to hardcode 1024x1024.
        val payload = SpacePayloads.forImageGen("flux-schnell-hf", "a woman in a red coat")
        val rendered = payload.map { it.toString() }
        assertTrue(rendered.contains("832"), "expected portrait width in payload: $rendered")
        assertTrue(rendered.contains("1216"), "expected portrait height in payload: $rendered")
    }

    @Test
    fun theFluxPayloadWidensForALandscapePrompt() {
        val payload = SpacePayloads.forImageGen("flux-schnell-hf", "panoramic city skyline")
        val rendered = payload.map { it.toString() }
        assertTrue(rendered.contains("1216"), "expected wide width in payload: $rendered")
        assertTrue(rendered.contains("832"), "expected short height in payload: $rendered")
    }
}
