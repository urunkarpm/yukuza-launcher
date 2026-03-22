package com.yukuza.launcher.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Spacing scale for Yukuza Launcher.
 *
 * Designed for the 10-foot TV experience — generous margins, clear breathing room.
 */
object YukuzaSpacing {
    // ── Base scale ────────────────────────────────────────────────────────────
    val xs   = 4.dp
    val sm   = 6.dp
    val md   = 8.dp
    val lg   = 12.dp
    val xl   = 16.dp
    val xxl  = 20.dp
    val xxxl = 24.dp

    // ── Layout ────────────────────────────────────────────────────────────────
    /** Horizontal screen edge margin */
    val screenHorizontal = 40.dp
    /** Top padding for the top bar row */
    val topBarTop        = 36.dp
    /** Vertical padding inside the bottom app strip */
    val appStripVertical = 20.dp
    /** Gap between widgets in the top-right cluster */
    val widgetSpacing    = 12.dp

    // ── Components ────────────────────────────────────────────────────────────
    /** GlassCard default internal horizontal padding */
    val cardHorizontal = 16.dp
    /** GlassCard default internal vertical padding */
    val cardVertical   = 14.dp
    /** ClockWidget — wider card padding */
    val clockHorizontal = 20.dp
    val clockVertical   = 16.dp
    /** NowPlaying widget internal padding */
    val nowPlayingHorizontal = 22.dp
    val nowPlayingVertical   = 20.dp

    // ── App icons ─────────────────────────────────────────────────────────────
    /** Width of each AppIcon column */
    val appIconWidth      = 88.dp
    /** Total icon container size (ring + halo) */
    val appIconSize       = 80.dp
    /** Inner drawable size */
    val appIconDrawable   = 64.dp
    /** Gap between AppIcon tiles in the LazyRow */
    val appIconRowSpacing = 20.dp
    /** Space between icon and label */
    val appIconLabelGap   = 8.dp

    // ── NowPlaying widget ─────────────────────────────────────────────────────
    val nowPlayingWidth    = 460.dp
    val albumArtSize       = 64.dp
    val progressBarHeight  = 2.dp
    val pulseDotSize       = 6.dp
}
