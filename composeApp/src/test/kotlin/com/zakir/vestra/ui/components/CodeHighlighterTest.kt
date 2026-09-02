package com.zakir.vestra.ui.components

import androidx.compose.ui.graphics.Color
import org.junit.Test
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue

/**
 * The one invariant that matters for a presentation-only tokenizer: **it never changes the code**.
 *
 * A wrong colour is cosmetic. A dropped brace, a swallowed string, or a duplicated line in a
 * snippet the user is about to copy into their editor is a real bug, and it is exactly the
 * failure mode a hand-rolled character scanner has. Every case here asserts the round trip.
 */
class CodeHighlighterTest {

    private val theme = CodeTheme(
        plain = Color(0xFF000001),
        keyword = Color(0xFF000002),
        string = Color(0xFF000003),
        number = Color(0xFF000004),
        comment = Color(0xFF000005),
        property = Color(0xFF000006),
        punctuation = Color(0xFF000007),
    )

    private fun roundTrips(code: String) {
        assertEquals("highlighting must not alter the source", code, CodeHighlighter.highlight(code, theme).text)
    }

    @Test
    fun `plain text round trips`() = roundTrips("hello world")

    @Test
    fun `css round trips`() = roundTrips(
        """
        .glass-card {
          background: rgba(139, 92, 246, 0.1);
          backdrop-filter: blur(24px);
        }
        """.trimIndent(),
    )

    @Test
    fun `kotlin round trips`() = roundTrips(
        """
        // builds a card
        fun card(name: String): String {
            val n = 42
            return "hi ${'$'}name"
        }
        """.trimIndent(),
    )

    @Test
    fun `unterminated string does not truncate the source`() = roundTrips("val a = \"never closed")

    @Test
    fun `unterminated block comment does not truncate the source`() = roundTrips("/* open forever\nmore")

    @Test
    fun `empty input yields empty output`() = roundTrips("")

    @Test
    fun `escaped quote inside a string round trips`() = roundTrips("""val s = "a \" b" ; done""")

    @Test
    fun `keywords and properties get different colours`() {
        val out = CodeHighlighter.highlight("val color: Blue", theme)
        val styles = out.spanStyles.associate { out.text.substring(it.start, it.end) to it.item.color }
        assertEquals("`val` is a keyword", theme.keyword, styles["val"])
        assertEquals("a word before ':' is a key", theme.property, styles["color"])
    }

    @Test
    fun `numbers keep their unit suffix as one token`() {
        val out = CodeHighlighter.highlight("blur(24px)", theme)
        val numberSpans = out.spanStyles.filter { it.item.color == theme.number }
        assertTrue(
            "a CSS length must colour as one token, not '24' plus stray letters",
            numberSpans.any { out.text.substring(it.start, it.end) == "24px" },
        )
    }

    @Test
    fun `comment runs to end of line only`() {
        val code = "// note\nval x = 1"
        val out = CodeHighlighter.highlight(code, theme)
        val commentSpans = out.spanStyles.filter { it.item.color == theme.comment }
        assertEquals(1, commentSpans.size)
        assertEquals("// note", out.text.substring(commentSpans[0].start, commentSpans[0].end))
    }
}
