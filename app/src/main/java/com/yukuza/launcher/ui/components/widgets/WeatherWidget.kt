package com.yukuza.launcher.ui.components.widgets

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yukuza.launcher.data.repository.WeatherRepository
import com.yukuza.launcher.domain.model.WeatherData
import com.yukuza.launcher.ui.components.glass.GlassCard

@Composable
fun WeatherWidget(
    data: WeatherData,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    GlassCard(modifier = modifier.padding(8.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clickable { onClick() }
                .padding(12.dp),
        ) {
            Column {
                Text(
                    text = "%.0f°C".format(data.tempCelsius),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White,
                )
                Text(
                    text = androidx.compose.ui.res.stringResource(WeatherRepository.wmoCodeToDescription(data.conditionCode)),
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.6f),
                )
                Text(
                    text = data.locationName,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(0.45f),
                )
            }
            if (data.isStale) {
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "~",
                    color = Color.Yellow.copy(0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.semantics { contentDescription = "Stale data" },
                )
            }
        }
    }
}
