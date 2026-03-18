package com.yukuza.launcher.ui.components.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yukuza.launcher.domain.model.AqiData
import com.yukuza.launcher.ui.components.glass.GlassCard

@Composable
fun AqiWidget(data: AqiData, modifier: Modifier = Modifier) {
    val categoryColor = when (data.category) {
        AqiData.AqiCategory.GOOD      -> Color(0xFF4CAF50)
        AqiData.AqiCategory.FAIR      -> Color(0xFFCDDC39)
        AqiData.AqiCategory.MODERATE  -> Color(0xFFFF9800)
        AqiData.AqiCategory.POOR      -> Color(0xFFF44336)
        AqiData.AqiCategory.VERY_POOR -> Color(0xFF9C27B0)
    }
    GlassCard(modifier = modifier.padding(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "AQI ${data.europeanAqi}",
                style = MaterialTheme.typography.bodyMedium,
                color = categoryColor,
            )
            Text(
                text = data.category.name.replace('_', ' '),
                style = MaterialTheme.typography.labelSmall,
                color = categoryColor.copy(0.7f),
            )
        }
    }
}
