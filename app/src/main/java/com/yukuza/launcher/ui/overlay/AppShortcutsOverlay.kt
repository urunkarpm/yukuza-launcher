package com.yukuza.launcher.ui.overlay

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import com.yukuza.launcher.domain.model.AppInfo
import com.yukuza.launcher.ui.components.glass.GlassCard
import kotlinx.collections.immutable.ImmutableList

@Composable
fun AppShortcutsOverlay(
    app: AppInfo,
    shortcuts: ImmutableList<ShortcutInfo>,
    onShortcutSelected: (ShortcutInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        GlassCard(modifier = Modifier.padding(8.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = app.label,
                    color = Color.White,
                )
                Spacer(Modifier.height(8.dp))
                shortcuts.take(5).forEach { shortcut ->
                    val label = shortcut.shortLabel?.toString() ?: ""
                    Text(
                        text = label,
                        color = Color.White.copy(alpha = 0.75f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShortcutSelected(shortcut) }
                            .focusable()
                            .padding(vertical = 8.dp, horizontal = 4.dp)
                            .semantics { contentDescription = label },
                    )
                }
            }
        }
    }
}
