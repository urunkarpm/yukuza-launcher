package com.yukuza.launcher.ui.components.widgets

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yukuza.launcher.ui.components.glass.GlassCard
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun ClockWidget(modifier: Modifier = Modifier) {
    val time by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(10_000)
        }
    }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm") }
    val ampm = if (time.hour < 12) "AM" else "PM"
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE · d MMMM yyyy")) }

    GlassCard(modifier = modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = time.format(timeFormatter),
                    style = MaterialTheme.typography.displayLarge,
                    color = Color.White,
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = ampm,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.45f),
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = today,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.6f),
            )
        }
    }
}
