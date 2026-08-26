package com.zakir.vestra.shared.util

/**
 * [Float.coerceIn] does not filter `NaN`/`Infinite` — for `x = NaN`, both `x < min` and
 * `x > max` evaluate `false`, so `coerceIn` returns `x` unchanged, still `NaN`. A `NaN` reaching
 * a Compose `Slider`/`Animatable` crashes with `IllegalArgumentException: current must not be
 * NaN`. Use this instead of a bare `coerceIn` for any value that ultimately feeds an animated
 * Compose value.
 */
fun Float.safeCoerceIn(min: Float, max: Float): Float =
    if (isNaN() || isInfinite()) (min + max) / 2f else coerceIn(min, max)
