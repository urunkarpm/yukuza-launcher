package com.yukuza.launcher.ui.components.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yukuza.launcher.ui.components.glass.GlassCard
import kotlinx.coroutines.delay

@Composable
fun ScreenTimerWidget(modifier: Modifier = Modifier) {
    val seconds by produceState(initialValue = 0L) {
        while (true) {
            delay(1_000)
            value++
        }
    }
    val minutes = seconds / 60
    val secs = seconds % 60
    GlassCard(modifier = modifier.padding(8.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "%d:%02d".format(minutes, secs),
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
            )
            Text(
                text = "Screen On",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.5f),
            )
        }
    }
}
