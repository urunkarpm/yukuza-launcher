package com.yukuza.launcher.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class AqiData(
    val europeanAqi: Int,
    val category: AqiCategory,
    val fetchedAt: Long,
    val isStale: Boolean = false,
) {
    enum class AqiCategory { GOOD, FAIR, MODERATE, POOR, VERY_POOR }
    companion object
}
