package com.zakir.vestra.ui.components

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The one invariant that matters for a presentation-only Markdown pass: **no visible text is
 * lost.**
 *
 * A missed bold is cosmetic — the user reads the sentence either way. A dropped clause because a
 * marker was unbalanced is real damage, and it is exactly what a hand-rolled scanner gets wrong.
 * The model's reply is the product; the renderer's job is to style it, never to edit it.
 *
 * So most cases here assert a round trip on the *stripped* source: parse, concatenate the block
 * text back together, and require every word to survive. The styling assertions are the smaller
 * half.
 */
class MarkdownTest {

    /** All text a render would show, in order. */
    private fun rendered(source: String): String =
        Markdown.parse(source).joinToString("\n") { it.spans.text }

    private fun assertKeepsWords(source: String, vararg words: String) {
        val out = rendered(source)
        words.forEach { word ->
            assertTrue("`$word` was dropped from: $out", out.contains(word))
        }
    }

    @Test
    fun `a bullet list becomes bullet blocks with the markers stripped`() {
        // The exact reply the shipped app rendered as literal "- **Fashion try-on** features".
        val blocks = Markdown.parse(
            """
            - **Fashion try-on** features and tips
            - **On-device AI** (Lite/Pro packs) vs cloud models
            """.trimIndent(),
        )
        assertEquals(2, blocks.size)
        assertTrue("first line is a bullet", blocks[0] is MarkdownBlock.Bullet)
        assertEquals("Fashion try-on features and tips", blocks[0].spans.text)
        assertEquals("On-device AI (Lite/Pro packs) vs cloud models", blocks[1].spans.text)
    }

    @Test
    fun `bold inside a bullet is styled, not printed`() {
        val spans = (Markdown.parse("- **Fashion try-on** features").single()).spans
        val bold = spans.spanStyles.firstOrNull { it.item.fontWeight == FontWeight.Bold }
        assertTrue("no bold span was produced", bold != null)
        assertEquals("Fashion try-on", spans.text.substring(bold!!.start, bold.end))
        assertTrue("asterisks leaked into the rendered text", !spans.text.contains("*"))
    }

    @Test
    fun `ordered items keep the number the model wrote`() {
        val blocks = Markdown.parse("1. first\n2. second\n10. tenth")
        val numbers = blocks.filterIsInstance<MarkdownBlock.Ordered>().map { it.number }
        assertEquals(listOf("1", "2", "10"), numbers)
    }

    @Test
    fun `headings carry their level`() {
        val blocks = Markdown.parse("# Big\n### Small")
        assertEquals(1, (blocks[0] as MarkdownBlock.Heading).level)
        assertEquals(3, (blocks[1] as MarkdownBlock.Heading).level)
        assertEquals("Big", blocks[0].spans.text)
    }

    @Test
    fun `soft-wrapped prose joins into one paragraph`() {
        val blocks = Markdown.parse("Welcome to The Lookbook.\nI can help you with:")
        assertEquals(1, blocks.size)
        assertEquals("Welcome to The Lookbook. I can help you with:", blocks[0].spans.text)
    }

    @Test
    fun `a blank line starts a new paragraph`() {
        assertEquals(2, Markdown.parse("first para\n\nsecond para").size)
    }

    @Test
    fun `an unclosed bold marker degrades to literal text rather than eating the line`() {
        assertKeepsWords("start **never closed and the rest of the sentence", "never closed", "rest of the sentence")
    }

    @Test
    fun `multiplication is not italic`() {
        val spans = Markdown.parse("cost is 2 * 3 * 4 dollars").single().spans
        assertTrue(
            "whitespace-hugging asterisks must stay literal",
            spans.spanStyles.none { it.item.fontStyle == FontStyle.Italic },
        )
        assertEquals("cost is 2 * 3 * 4 dollars", spans.text)
    }

    @Test
    fun `an unclosed backtick keeps the rest of the line`() {
        assertKeepsWords("run `npm install and then start", "npm install", "then start")
    }

    @Test
    fun `a link renders its label and drops only the target`() {
        val spans = Markdown.parse("see [the docs](https://example.com) for more").single().spans
        assertEquals("see the docs for more", spans.text)
    }

    @Test
    fun `a malformed link is left alone rather than swallowed`() {
        assertKeepsWords("an array like [1, 2, 3] and text after", "1, 2, 3", "text after")
    }

    @Test
    fun `nested emphasis inside bold survives`() {
        val spans = Markdown.parse("**bold with *italic* inside**").single().spans
        assertEquals("bold with italic inside", spans.text)
        assertTrue("bold missing", spans.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue("italic missing", spans.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }

    @Test
    fun `empty input yields no blocks rather than a blank one`() {
        assertEquals(0, Markdown.parse("").size)
        assertEquals(0, Markdown.parse("   \n\n  ").size)
    }

    @Test
    fun `a whole realistic reply keeps every word`() {
        assertKeepsWords(
            """
            Hi there! 👋

            Welcome to The Lookbook. I can help you with:

            - **Fashion try-on** features and tips
            - **On-device AI** (Lite/Pro packs) vs cloud models
            - **Headlines & updates** about the app
            - General questions about how things work

            What can I help you with today?
            """.trimIndent(),
            "Hi there!",
            "Welcome to The Lookbook",
            "Fashion try-on",
            "Lite/Pro packs",
            "Headlines & updates",
            "General questions about how things work",
            "What can I help you with today?",
        )
    }
}

/**
 * The top bar's model-name trim.
 *
 * Tiny, but it guards a defect the renders actually showed: "Bonsai Image 4B (LiteRT)" arriving
 * as "Bonsai Image 4B (Li…". The rule has to drop the *runtime* and never the model number.
 */
class ShortModelNameTest {

    private fun short(label: String) = com.zakir.vestra.ui.screens.home.shortModelName(label)

    @Test
    fun `a status tail is dropped`() {
        // The exact string the shipped build rendered into the top bar.
        assertEquals("FLUX.1 Schnell", short("FLUX.1 Schnell · Ready · verified just now"))
        assertEquals("Bonsai Image 4B", short("Bonsai Image 4B (LiteRT) · Ready offline"))
    }

    @Test
    fun `a trailing runtime parenthetical is dropped`() {
        assertEquals("Llama 3.3 70B Versatile", short("Llama 3.3 70B Versatile (Groq)"))
        assertEquals("Bonsai Image 4B", short("Bonsai Image 4B (LiteRT)"))
    }

    @Test
    fun `a name with no suffix is untouched`() {
        assertEquals("FLUX.1 Schnell", short("FLUX.1 Schnell"))
    }

    @Test
    fun `a parenthetical that is not at the end stays`() {
        // Only a *trailing* group is runtime noise. One in the middle is part of the name.
        assertEquals("SDXL (turbo) refiner", short("SDXL (turbo) refiner"))
    }

    @Test
    fun `a name that is nothing but a suffix is left alone`() {
        // Trimming to an empty string would render a selector with no label at all.
        assertEquals("(LiteRT)", short("(LiteRT)"))
        assertEquals("· Ready", short("· Ready"))
    }
}
