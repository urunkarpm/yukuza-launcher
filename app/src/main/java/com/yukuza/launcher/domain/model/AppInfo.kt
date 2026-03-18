package com.yukuza.launcher.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class AppInfo(
    val packageName: String,
    val label: String,
    val order: Int,
    val dominantColor: Color = Color.White,
)
