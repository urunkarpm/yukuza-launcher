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
    val timeFormatter = remember { DateTimeFormatter.ofPattern("h:mm", java.util.Locale.getDefault()) }
    val ampmFormatter = remember { DateTimeFormatter.ofPattern("a", java.util.Locale.getDefault()) }
    val ampm = time.format(ampmFormatter)
    val today = remember { LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE · d MMMM yyyy", java.util.Locale.getDefault())) }

    GlassCard(
        modifier = modifier,
        elevation = 12f,
        shape = RoundedCornerShape(20.dp),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp, vertical = 18.dp)
                .background(
                    brush = androidx.compose.ui.graphics.Brush.linearGradient(
                        colors = listOf(
                            YukuzaColors.ClockGradientStart.copy(alpha = 0.15f),
                            YukuzaColors.ClockGradientEnd.copy(alpha = 0.08f),
                        ),
                        begin = androidx.compose.ui.geometry.Offset.Zero,
                        end = androidx.compose.ui.geometry.Offset.Infinite,
                    ),
                )
                .padding(horizontal = 24.dp, vertical = 18.dp),
        ) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = time.format(timeFormatter),
                    style = MaterialTheme.typography.displayLarge,
                    color = YukuzaColors.TextPrimary,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Light,
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = ampm,
                    style = MaterialTheme.typography.labelMedium,
                    color = YukuzaColors.TextSecondary,
                    modifier = Modifier.padding(bottom = 8.dp),
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Medium,
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = today,
                style = MaterialTheme.typography.bodyMedium,
                color = YukuzaColors.TextTertiary,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Normal,
            )
        }
    }
}
