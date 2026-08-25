package com.zakir.vestra.ui.theme

import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pure-JVM guard on the derived spacing scale — every value must be positive and the scale must
 * actually ascend, or components that lean on relative sizing (e.g. `xs` < `section` < `xxl`)
 * would silently render out of order.
 */
class SpacingTokensTest {

    @Test
    fun allTokensArePositive() {
        listOf(
            SpacingTokens.xxs,
            SpacingTokens.xs,
            SpacingTokens.sm,
            SpacingTokens.md,
            SpacingTokens.lg,
            SpacingTokens.xl,
            SpacingTokens.xxl,
            SpacingTokens.section,
        ).forEach { token ->
            assertTrue("$token must be > 0", token.value > 0f)
        }
    }

    @Test
    fun coreScaleAscends() {
        assertTrue(SpacingTokens.xxs < SpacingTokens.xs)
        assertTrue(SpacingTokens.xs < SpacingTokens.sm)
        assertTrue(SpacingTokens.sm < SpacingTokens.md)
        assertTrue(SpacingTokens.md < SpacingTokens.lg)
        assertTrue(SpacingTokens.lg < SpacingTokens.xl)
        assertTrue(SpacingTokens.xl < SpacingTokens.xxl)
    }

    @Test
    fun sectionSitsBetweenMdAndLg() {
        // `section` is the historical 18.dp GlassCard padding, kept as its own token rather than
        // folded into `md`/`lg` so existing call sites read as an intentional choice, not a typo.
        assertTrue(SpacingTokens.section > SpacingTokens.md)
        assertTrue(SpacingTokens.section < SpacingTokens.lg)
    }
}
