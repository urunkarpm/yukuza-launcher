package com.yukuza.launcher.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.yukuza.launcher.ui.components.aurora.AuroraBackground
import kotlinx.coroutines.delay
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@Composable
fun AmbientScreensaver(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val time by produceState(initialValue = LocalTime.now()) {
        while (true) {
            value = LocalTime.now()
            delay(10_000)
        }
    }
    val timeFormatter = java.time.format.DateTimeFormatter.ofPattern("h:mm", java.util.Locale.getDefault())
    val ampmFormatter = java.time.format.DateTimeFormatter.ofPattern("a", java.util.Locale.getDefault())

    Box(
        modifier = modifier
            .fillMaxSize(),
    ) {
        AuroraBackground()
        Text(
            text = "${time.format(timeFormatter)} ${time.format(ampmFormatter)}",
            style = MaterialTheme.typography.displayLarge,
            color = Color.White.copy(alpha = 0.85f),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(16.dp),
        )
    }
}
