package com.yukuza.launcher.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner-radius tokens for Yukuza Launcher.
 *
 * Keep the hierarchy consistent:
 *   cards / overlays → 12 dp
 *   app icon container → 20 dp
 *   app icon inner drawable → 14 dp
 *   small pills / badges → 8 dp
 */
object YukuzaShapes {
    /** GlassCard, overlay panels, city picker rows */
    val card     = RoundedCornerShape(12.dp)
    /** AppIcon outer container (ring + halo) */
    val appIcon  = RoundedCornerShape(20.dp)
    /** AppIcon inner drawable clip */
    val appIconInner = RoundedCornerShape(14.dp)
    /** Album art in NowPlayingWidget */
    val albumArt = RoundedCornerShape(10.dp)
    /** Small pill buttons, badges */
    val pill     = RoundedCornerShape(8.dp)
    /** Fully circular (dots, toggle knobs) */
    val circle   = RoundedCornerShape(50)
}

/** Material3 Shapes wired from Yukuza tokens for MaterialTheme. */
val YukuzaM3Shapes = Shapes(
    extraSmall = YukuzaShapes.pill,
    small      = YukuzaShapes.card,
    medium     = YukuzaShapes.card,
    large      = YukuzaShapes.appIcon,
    extraLarge = YukuzaShapes.appIcon,
)
