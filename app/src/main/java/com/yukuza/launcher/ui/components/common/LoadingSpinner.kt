package com.yukuza.launcher.ui.components.common

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.tv.material3.CircularProgressIndicator
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme

/**
 * A reusable loading spinner component for the launcher.
 * Provides consistent styling across all loading states.
 */
@Composable
fun LoadingSpinner(
    modifier: Modifier = Modifier,
    color: androidx.compose.ui.graphics.Color = LocalContentColor.current,
    trackColor: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.1f),
) {
    CircularProgressIndicator(
        modifier = modifier,
        color = color,
        trackColor = trackColor,
    )
}
