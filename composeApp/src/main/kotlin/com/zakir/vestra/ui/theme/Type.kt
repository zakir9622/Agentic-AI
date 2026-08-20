package com.zakir.vestra.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

// Futuristic atelier: geometric grotesque for UI, condensed display for brand moments.
private val Display = FontFamily.SansSerif
private val Body = FontFamily.SansSerif

val VestraTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Light,
        fontSize = 48.sp,
        lineHeight = 1.05.em,
        letterSpacing = (-0.03).em,
    ),
    displayMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Light,
        fontSize = 36.sp,
        lineHeight = 1.1.em,
        letterSpacing = (-0.02).em,
    ),
    displaySmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 28.sp,
        lineHeight = 1.12.em,
        letterSpacing = (-0.015).em,
    ),
    headlineLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp,
        lineHeight = 1.15.em,
        letterSpacing = (-0.01).em,
    ),
    headlineMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.SemiBold,
        fontSize = 22.sp,
        lineHeight = 1.2.em,
    ),
    titleLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 1.25.em,
    ),
    titleMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 1.3.em,
    ),
    bodyLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 1.5.em,
    ),
    bodyMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 1.45.em,
    ),
    labelLarge = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 12.sp,
        letterSpacing = 0.14.em,
    ),
    labelMedium = TextStyle(
        fontFamily = Body,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        letterSpacing = 0.12.em,
    ),
)
