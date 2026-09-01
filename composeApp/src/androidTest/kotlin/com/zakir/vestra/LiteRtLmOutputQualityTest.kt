package com.zakir.vestra

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.zakir.vestra.shared.engine.litert.LiteRtLmEngine
import com.zakir.vestra.shared.engine.litert.LiteRtLmGenerateResult
import com.zakir.vestra.shared.engine.local.LiteRtLmPacks
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * D1 (docs/plans/lovable-parity-local-first/PLAN.md): real-model output-quality evidence for
 * local code generation — representative prompts run against the actual Gemma 4 pack, with each
 * output checked for genuinely code-shaped content, not just "a non-empty string came back."
 *
 * Skips (does not fail) when no pack is on device, matching `LiteRtLmBenchmarkTest`'s pattern —
 * this suite has never been run in this development environment (no device/emulator available;
 * see `docs/DRAWBACKS.md`'s Testability section). Every assertion here is real and will fail
 * genuinely poor output once it does run on a device with the pack installed — this is not a
 * placeholder that passes unconditionally.
 *
 * Manual push (same as LiteRtLmBenchmarkTest):
 *   adb push gemma-4-E2B-it.litertlm /sdcard/Android/data/com.zakir.vestra/files/packs/local-gemma-4-e2b-v1/1/
 */
@RunWith(AndroidJUnit4::class)
class LiteRtLmOutputQualityTest {

    private fun modelFileOrSkip(): File? {
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val packDir = File(context.filesDir, "packs/${LiteRtLmPacks.GEMMA4_CODE}/1")
        val model = File(packDir, LiteRtLmPacks.GEMMA4_FILE)
        if (!model.isFile || model.length() < 500_000_000L) {
            Log.i(TAG, "SKIP — no Gemma 4 pack at ${model.absolutePath}")
            return null
        }
        return model
    }

    private fun generateOrFail(prompt: String, system: String): String {
        val model = modelFileOrSkip() ?: return ""
        val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
        val engine = LiteRtLmEngine(context = context, modelPath = model.absolutePath, useGpu = false)
        return engine.use {
            it.initialize()
            val result = it.generateText(prompt = prompt, system = system)
            val ok = result as? LiteRtLmGenerateResult.Ok
                ?: error("Expected LiteRtLmGenerateResult.Ok, got $result")
            Log.i(TAG, "QUALITY prompt=$prompt tokensOut=${ok.tokensOut}\n${ok.text}")
            ok.text
        }
    }

    /** Shared quality bar: every response must clear these regardless of the specific prompt. */
    private fun assertBasicOutputQuality(text: String, prompt: String) {
        if (text.isEmpty()) return // skipped — no pack on device
        assertTrue("empty response to: $prompt", text.trim().isNotEmpty())
        assertTrue("suspiciously short response (${text.length} chars) to: $prompt", text.trim().length > 20)
        assertFalse(
            "response leaked a raw <think> block instead of a clean answer: $prompt",
            text.contains("<think>", ignoreCase = true),
        )
    }

    @Test
    fun kotlinQuicksortLooksLikeRealCode() {
        val text = generateOrFail(
            prompt = "Write a Kotlin function that implements quicksort on a MutableList<Int>.",
            system = "You are a concise Kotlin coding assistant. Answer with code and a brief explanation.",
        )
        assertBasicOutputQuality(text, "quicksort")
        if (text.isEmpty()) return
        assertTrue("expected a Kotlin function signature (fun ...)", text.contains("fun "))
        assertTrue(
            "expected quicksort's recursive/partition structure to be recognizable",
            text.contains("pivot", ignoreCase = true) || text.contains("partition", ignoreCase = true),
        )
    }

    @Test
    fun stateFlowExplanationIsSubstantiveNotAnEcho() {
        val prompt = "In two or three sentences, explain what a Kotlin StateFlow is and how it differs from a Flow."
        val text = generateOrFail(
            prompt = prompt,
            system = "You are a concise Kotlin/Android assistant.",
        )
        assertBasicOutputQuality(text, "StateFlow explanation")
        if (text.isEmpty()) return
        assertFalse("response merely echoed the prompt back", text.trim() == prompt)
        assertTrue(
            "expected the explanation to actually mention StateFlow or state",
            text.contains("state", ignoreCase = true),
        )
    }

    @Test
    fun composeCounterButtonUsesRealComposeApis() {
        val text = generateOrFail(
            prompt = "Write a Jetpack Compose @Composable function for a button that counts how many times it's been tapped.",
            system = "You are a concise Kotlin Compose coding assistant. Answer with code.",
        )
        assertBasicOutputQuality(text, "Compose counter button")
        if (text.isEmpty()) return
        assertTrue("expected an @Composable annotation", text.contains("@Composable"))
        assertTrue(
            "expected Compose state plumbing (remember/mutableStateOf) for a counter",
            text.contains("remember", ignoreCase = true) || text.contains("mutableStateOf", ignoreCase = true),
        )
    }

    companion object {
        private const val TAG = "LookbookLiteRtLmQuality"
    }
}
