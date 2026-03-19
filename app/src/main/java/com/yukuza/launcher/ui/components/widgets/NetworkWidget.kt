package com.yukuza.launcher.ui.components.widgets

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yukuza.launcher.domain.model.NetworkData
import com.yukuza.launcher.ui.components.glass.GlassCard

@Composable
fun NetworkWidget(data: NetworkData, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    GlassCard(
        modifier = modifier
            .padding(8.dp)
            .clickable {
                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
            }
            .semantics { contentDescription = context.getString(com.yukuza.launcher.R.string.network_content_description) },
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.network_speed_format, data.speedMbps),
                style = MaterialTheme.typography.bodyMedium,
                color = if (data.isConnected) Color.White else Color.Red.copy(0.7f),
            )
            Text(
                text = if (data.isConnected) androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.network_wifi) else androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.network_offline),
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(0.5f),
            )
        }
    }
}
