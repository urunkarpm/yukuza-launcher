package com.yukuza.launcher.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Roboto is the Android system sans-serif — FontFamily.SansSerif maps directly to it
private val Roboto = FontFamily.SansSerif

/**
 * TV-optimised typography scale for Yukuza Launcher.
 *
 * Viewing distance is ~2 m, so minimum legible size is 14 sp and display
 * sizes go up to 72 sp for the clock.  Letter spacing is widened on
 * display styles to improve readability on bright-backlit panels.
 */
val YukuzaTypography = Typography(
    // ── Display ───────────────────────────────────────────────────────────────
    /** Clock time: 72 sp, ultra-light — the hero element on screen */
    displayLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W200,
        fontSize = 72.sp,
        letterSpacing = 5.sp,
    ),
    /** Large overlay headings, now-playing track title at wider context */
    displayMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W300,
        fontSize = 48.sp,
        letterSpacing = 1.sp,
    ),
    /** Section headers inside overlays */
    displaySmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W300,
        fontSize = 36.sp,
        letterSpacing = 0.5.sp,
    ),

    // ── Headline ──────────────────────────────────────────────────────────────
    /** Quick-settings overlay section titles */
    headlineLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 24.sp,
        letterSpacing = 0.sp,
    ),
    /** City picker dialog title */
    headlineMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 20.sp,
        letterSpacing = 0.sp,
    ),
    headlineSmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontSize = 18.sp,
        letterSpacing = 0.sp,
    ),

    // ── Body ──────────────────────────────────────────────────────────────────
    /** Primary readable content — widget values, track title */
    bodyLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 16.sp,
        letterSpacing = 0.sp,
    ),
    /** Default body — widget labels, city names */
    bodyMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 14.sp,
        letterSpacing = 0.sp,
    ),
    /** Compact body — secondary widget lines */
    bodySmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
    ),

    // ── Label ─────────────────────────────────────────────────────────────────
    /** Slider / toggle labels inside Quick Settings */
    labelLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontSize = 13.sp,
        letterSpacing = 0.sp,
    ),
    /** App icon labels, widget sub-labels */
    labelMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 12.sp,
        letterSpacing = 0.sp,
    ),
    /** Date line, AM/PM, AQI category, network status */
    labelSmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W400,
        fontSize = 11.sp,
        letterSpacing = 0.sp,
    ),

    // ── Title ─────────────────────────────────────────────────────────────────
    /** Used by Material3 ListItem and similar scaffolding */
    titleLarge = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontSize = 22.sp,
        letterSpacing = 0.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontSize = 16.sp,
        letterSpacing = 0.15.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Roboto,
        fontWeight = FontWeight.W500,
        fontSize = 14.sp,
        letterSpacing = 0.1.sp,
    ),
)
