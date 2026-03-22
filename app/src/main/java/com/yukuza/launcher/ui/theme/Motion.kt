package com.yukuza.launcher.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing

/**
 * Motion tokens for Yukuza Launcher.
 *
 * Durations are tuned for TV remote interaction — snappy focus transitions,
 * gentle ambient loops.
 */
object YukuzaMotion {
    // ── Durations (ms) ────────────────────────────────────────────────────────

    /** AppIcon scale-up / scale-down on D-pad focus */
    const val FOCUS_SCALE_MS     = 100
    /** Fade in/out for short transitions (ring, label alpha) — use snap() */
    const val SNAP_MS            = 0
    /** Now Playing widget enter/exit */
    const val NOW_PLAYING_MS     = 300
    /** Album art spin period — one full rotation */
    const val ALBUM_SPIN_MS      = 8_000
    /** NowPlaying pulse dot — one half-cycle */
    const val PULSE_MS           = 800
    /** Aurora blob drift — per blob, used as a multiplier base */
    const val AURORA_BASE_MS     = 16_000
    /** Quick settings overlay slide-in */
    const val OVERLAY_ENTER_MS   = 250
    /** City picker popup fade */
    const val POPUP_ENTER_MS     = 200

    // ── Easings ───────────────────────────────────────────────────────────────

    /** Standard Material decelerate — used for focus scale */
    val focusEasing: Easing = FastOutSlowInEasing
    /** Smooth ambient ease — used for aurora, progress bars */
    val ambientEasing: Easing = CubicBezierEasing(0.45f, 0f, 0.55f, 1f)
    /** Quick snap for instant state changes (saturation, ring) */
    val snapEasing: Easing = CubicBezierEasing(0f, 0f, 1f, 1f)

    // ── Initial scale for animated visibility ─────────────────────────────────
    const val ENTER_INITIAL_SCALE = 0.95f
}
