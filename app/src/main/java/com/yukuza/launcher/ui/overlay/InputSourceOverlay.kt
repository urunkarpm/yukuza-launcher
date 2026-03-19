package com.yukuza.launcher.ui.overlay

import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.yukuza.launcher.ui.components.glass.GlassCard

data class InputSource(val id: Int, val label: String)

@Composable
fun InputSourceOverlay(
    sources: List<InputSource>,
    onSourceSelected: (InputSource) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        GlassCard(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.input_source_title), color = Color.White)
                if (sources.isEmpty()) {
                    Text(
                        androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.no_hdmi_sources),
                        color = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.padding(top = 8.dp),
                    )
                } else {
                    sources.forEach { source ->
                        Text(
                            text = source.label,
                            color = Color.White.copy(alpha = 0.75f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSourceSelected(source) }
                                .focusable()
                                .padding(vertical = 8.dp, horizontal = 4.dp)
                                .semantics { contentDescription = source.label },
                        )
                    }
                }
            }
        }
    }
}
