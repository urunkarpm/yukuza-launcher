package com.yukuza.launcher.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color

@Composable
fun NightModeOverlay(
    isActive: Boolean,
    modifier: Modifier = Modifier,
) {
    val overlayColor by animateColorAsState(
        targetValue = if (isActive) Color.Black.copy(alpha = 0.30f) else Color.Transparent,
        animationSpec = tween(durationMillis = 1500),
        label = "nightModeOverlay",
    )
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(overlayColor),
    )
}
