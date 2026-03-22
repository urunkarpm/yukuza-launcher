package com.yukuza.launcher.ui.components.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yukuza.launcher.domain.model.AqiData
import com.yukuza.launcher.ui.components.glass.GlassCard
import com.yukuza.launcher.ui.theme.YukuzaColors
import com.yukuza.launcher.ui.theme.YukuzaSpacing

@Composable
fun AqiWidget(data: AqiData, modifier: Modifier = Modifier) {
    val categoryColor = when (data.category) {
        AqiData.AqiCategory.GOOD      -> YukuzaColors.AqiGood
        AqiData.AqiCategory.FAIR      -> YukuzaColors.AqiFair
        AqiData.AqiCategory.MODERATE  -> YukuzaColors.AqiModerate
        AqiData.AqiCategory.POOR      -> YukuzaColors.AqiPoor
        AqiData.AqiCategory.VERY_POOR -> YukuzaColors.AqiVeryPoor
    }
    GlassCard(modifier = modifier) {
        Column(modifier = Modifier.padding(horizontal = YukuzaSpacing.cardHorizontal, vertical = YukuzaSpacing.cardVertical)) {
            val categoryNameRes = when (data.category) {
                AqiData.AqiCategory.GOOD -> com.yukuza.launcher.R.string.aqi_good
                AqiData.AqiCategory.FAIR -> com.yukuza.launcher.R.string.aqi_fair
                AqiData.AqiCategory.MODERATE -> com.yukuza.launcher.R.string.aqi_moderate
                AqiData.AqiCategory.POOR -> com.yukuza.launcher.R.string.aqi_poor
                AqiData.AqiCategory.VERY_POOR -> com.yukuza.launcher.R.string.aqi_very_poor
            }
            Text(
                text = androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.aqi_value, data.europeanAqi),
                style = MaterialTheme.typography.bodyMedium,
                color = categoryColor,
            )
            Text(
                text = androidx.compose.ui.res.stringResource(categoryNameRes),
                style = MaterialTheme.typography.labelSmall,
                color = categoryColor.copy(0.7f),
            )
        }
    }
}
