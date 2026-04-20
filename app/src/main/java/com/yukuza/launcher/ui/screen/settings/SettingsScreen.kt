package com.yukuza.launcher.ui.screen.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Switch
import com.yukuza.launcher.R
import com.yukuza.launcher.ui.components.aurora.AuroraBackground

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    AuroraBackground()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(60.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineLarge,
            color = androidx.compose.ui.graphics.Color.White,
        )

        Spacer(Modifier.height(40.dp))

        // Appearance section
        SettingsSection(title = stringResource(R.string.settings_section_appearance)) {
            SettingsSwitchItem(
                label = stringResource(R.string.settings_dark_mode),
                description = stringResource(R.string.settings_dark_mode_description),
                checked = uiState.darkModeEnabled,
                onCheckedChange = { viewModel.toggleDarkMode(it) },
            )
            SettingsSwitchItem(
                label = stringResource(R.string.settings_aurora_animation),
                description = stringResource(R.string.settings_aurora_animation_description),
                checked = uiState.auroraAnimationEnabled,
                onCheckedChange = { viewModel.toggleAuroraAnimation(it) },
            )
            SettingsSwitchItem(
                label = stringResource(R.string.settings_ambient_mode),
                description = stringResource(R.string.settings_ambient_mode_description),
                checked = uiState.ambientModeEnabled,
                onCheckedChange = { viewModel.toggleAmbientMode(it) },
            )
        }

        Spacer(Modifier.height(24.dp))

        // Widgets section
        SettingsSection(title = stringResource(R.string.settings_section_widgets)) {
            SettingsSwitchItem(
                label = stringResource(R.string.settings_weather_widget),
                description = stringResource(R.string.settings_weather_widget_description),
                checked = uiState.weatherWidgetEnabled,
                onCheckedChange = { viewModel.toggleWeatherWidget(it) },
            )
            SettingsSwitchItem(
                label = stringResource(R.string.settings_now_playing_widget),
                description = stringResource(R.string.settings_now_playing_widget_description),
                checked = uiState.nowPlayingWidgetEnabled,
                onCheckedChange = { viewModel.toggleNowPlayingWidget(it) },
            )
            SettingsSwitchItem(
                label = stringResource(R.string.settings_network_widget),
                description = stringResource(R.string.settings_network_widget_description),
                checked = uiState.networkSpeedWidgetEnabled,
                onCheckedChange = { viewModel.toggleNetworkSpeedWidget(it) },
            )
        }

        Spacer(Modifier.height(24.dp))

        // System section
        SettingsSection(title = stringResource(R.string.settings_section_system)) {
            SettingsSwitchItem(
                label = stringResource(R.string.settings_auto_update),
                description = stringResource(R.string.settings_auto_update_description),
                checked = uiState.autoUpdateEnabled,
                onCheckedChange = { viewModel.toggleAutoUpdate(it) },
            )
        }

        Spacer(Modifier.weight(1f))

        // Back button hint
        Text(
            text = stringResource(R.string.settings_press_back_to_exit),
            style = MaterialTheme.typography.bodySmall,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit,
) {
    Column {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 16.dp),
        )
        content()
    }
}

@Composable
private fun SettingsSwitchItem(
    label: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = androidx.compose.ui.graphics.Color.White,
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.6f),
            )
        }
        Spacer(Modifier.width(24.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
        )
    }
}
