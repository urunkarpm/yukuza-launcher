package com.yukuza.launcher.ui.theme

import androidx.compose.ui.graphics.Color

object YukuzaColors {
    // ── Aurora palette ────────────────────────────────────────────────────────
    val AuroraPurple = Color(0xFF5832FA)
    val AuroraTeal   = Color(0xFF10B4A0)
    val AuroraPink   = Color(0xFFE63C91)
    val AuroraBlue   = Color(0xFF32B4F5)

    // ── Backgrounds ───────────────────────────────────────────────────────────
    /** Main canvas — deep space with a purple tint */
    val DeepBlack       = Color(0xFF06030F)
    /** App strip background — slightly lighter near-black */
    val AppStripSurface = Color(0xD004020C)

    // ── Glass morphism ────────────────────────────────────────────────────────
    val GlassSurface = Color(0x0A060210)
    val GlassBorder  = Color(0x24FFFFFF)
    /** Subtle purple border used on the app strip bottom bar */
    val AppStripBorder = Color(0xFF8632FA).copy(alpha = 0.22f)

    // ── Accent / glow ─────────────────────────────────────────────────────────
    val DefaultGlow      = Color(0xFF14B8A6)
    val NowPlayingGreen  = Color(0xFF1DB954)  // Spotify-style playback green

    // ── Air Quality Index ─────────────────────────────────────────────────────
    val AqiGood      = Color(0xFF4CAF50)
    val AqiFair      = Color(0xFFCDDC39)
    val AqiModerate  = Color(0xFFFF9800)
    val AqiPoor      = Color(0xFFF44336)
    val AqiVeryPoor  = Color(0xFF9C27B0)

    // ── Content / text alphas ─────────────────────────────────────────────────
    /** Primary content — fully opaque white */
    val ContentPrimary   = Color.White
    /** Secondary labels, subtitles */
    val ContentSecondary = Color.White.copy(alpha = 0.60f)
    /** Tertiary — hints, AM/PM marker, etc. */
    val ContentTertiary  = Color.White.copy(alpha = 0.45f)
    /** Quaternary — very faint supplemental text */
    val ContentQuaternary = Color.White.copy(alpha = 0.40f)
    /** Dimmed app labels when not focused */
    val ContentDimmed    = Color.White.copy(alpha = 0.50f)
    /** Progress-track fill / ghost layer */
    val ContentGhost     = Color.White.copy(alpha = 0.12f)

    // ── States ────────────────────────────────────────────────────────────────
    val NetworkOffline = Color(0xFFEF4444)
}
