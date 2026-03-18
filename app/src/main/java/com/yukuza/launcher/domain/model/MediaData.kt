package com.yukuza.launcher.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class MediaData(
    val trackTitle: String,
    val artist: String,
    val albumArtUri: String?,
    val elapsedMs: Long,
    val durationMs: Long,
    val isPlaying: Boolean,
    val sourceAppLabel: String,
    val dominantColor: Color = Color(0xFF14B8A6),
)
