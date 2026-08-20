package com.zakir.vestra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Futuristic atelier: geometric grotesque. Line heights must be Sp (not Em) —
// Material3 OutlinedTextField lerps label styles and crashes on Em↔Sp mix.
private val Display = FontFamily.SansSerif
private val Body = FontFamily.SansSerif

val VestraTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        lineHeight = 52.sp,
        letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Light,
        fontSize = 36.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.3).sp,
    ),
    displaySmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.2).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.2.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.0.sp,
    ),
)
