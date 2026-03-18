package com.yukuza.launcher.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class NetworkData(
    val speedMbps: Float,
    val isConnected: Boolean,
)
