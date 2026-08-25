package com.zakir.vestra.shared.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ChangelogParserTest {

    @Test
    fun parse_emptyString_returnsEmptyList() {
        assertTrue(ChangelogParser.parse("").isEmpty())
    }

    @Test
    fun parse_textBeforeFirstHeading_isIgnored() {
        val markdown = "# Changelog — The Lookbook\n\nSome intro text.\n\n## 1.0.0\nFirst release.\n"
        val releases = ChangelogParser.parse(markdown)
        assertEquals(1, releases.size)
        assertEquals("1.0.0", releases[0].version)
        assertEquals("First release.", releases[0].body)
    }

    @Test
    fun parse_singleRelease_capturesVersionAndBody() {
        val markdown = "## 2.0.0\nLine one.\nLine two.\n"
        val releases = ChangelogParser.parse(markdown)
        assertEquals(1, releases.size)
        assertEquals("2.0.0", releases[0].version)
        assertEquals("Line one.\nLine two.", releases[0].body)
    }

    @Test
    fun parse_multipleReleases_splitsCorrectly() {
        val markdown = "## 2.0.0\nSecond.\n\n## 1.0.0\nFirst.\n"
        val releases = ChangelogParser.parse(markdown)
        assertEquals(2, releases.size)
        assertEquals("2.0.0", releases[0].version)
        assertEquals("Second.", releases[0].body)
        assertEquals("1.0.0", releases[1].version)
        assertEquals("First.", releases[1].body)
    }

    @Test
    fun parse_versionWithParentheticalQualifier_keptAsIs() {
        val markdown = "## 3.1.0 (stable)\nStable release.\n"
        val releases = ChangelogParser.parse(markdown)
        assertEquals("3.1.0 (stable)", releases[0].version)
    }

    @Test
    fun parse_bodyPreservesInternalStructure() {
        val markdown = "## 1.0.0\n- Item one\n- Item two\n\nMore text.\n"
        val releases = ChangelogParser.parse(markdown)
        assertTrue(releases[0].body.contains("- Item one"))
        assertTrue(releases[0].body.contains("- Item two"))
        assertTrue(releases[0].body.contains("More text."))
    }

    @Test
    fun parse_trailingWhitespaceInBody_isTrimmed() {
        val markdown = "## 1.0.0\nBody text.\n\n\n"
        val releases = ChangelogParser.parse(markdown)
        assertEquals("Body text.", releases[0].body)
    }

    @Test
    fun parse_nonVersionHeading_isDroppedNotAttributedToAdjacentRelease() {
        // Regression: this repo's real CHANGELOG.md has a non-version "## CI / releases" note
        // sandwiched between two version sections — it must not be reported as a fake release,
        // nor silently folded into whichever version precedes it.
        val markdown = """
            |## 3.1.0-rc10
            |Five-star Q1 work.
            |
            |## CI / releases
            |Release APK only on main.
            |
            |## 3.1.0-rc9
            |Pack device handshake.
        """.trimMargin()
        val releases = ChangelogParser.parse(markdown)
        assertEquals(listOf("3.1.0-rc10", "3.1.0-rc9"), releases.map { it.version })
        assertEquals("Five-star Q1 work.", releases[0].body)
        assertTrue(!releases[0].body.contains("Release APK only on main"))
    }

    @Test
    fun parse_realChangelogFixture_producesNonEmptyOrderedReleases() {
        // A representative excerpt shaped exactly like this repo's real CHANGELOG.md — verifies
        // the parser handles the actual heading/body conventions used there (backticked code
        // spans, bold markers, nested bullet lists), not just synthetic minimal input.
        val markdown = """
            |# Changelog — The Lookbook
            |
            |## 3.1.3
            |Phase 1 (A0/A1/A2) of `docs/plans/lookbookweb-exact-ui-parity/PLAN.md` — the design-system
            |foundation for matching lookbookweb.lovable.app exactly.
            |
            |- **A0 — full color/radius token replacement.** Some detail here.
            |- **A1 — the missing motion primitives.** More detail.
            |
            |## 3.1.2
            |Two follow-ups requested after 3.1.1 shipped.
            |
            |- **Real "Test connection" checks in Settings.**
            |
            |## 3.1.1
            |Initial port of GoogleLookBookUI pieces.
        """.trimMargin()
        val releases = ChangelogParser.parse(markdown)
        assertEquals(listOf("3.1.3", "3.1.2", "3.1.1"), releases.map { it.version })
        assertTrue(releases[0].body.contains("A0 — full color/radius token replacement"))
        assertTrue(releases[1].body.contains("Real \"Test connection\" checks"))
        assertTrue(releases[2].body.contains("Initial port of GoogleLookBookUI"))
    }
}
