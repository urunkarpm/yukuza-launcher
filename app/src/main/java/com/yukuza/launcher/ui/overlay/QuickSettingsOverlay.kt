package com.yukuza.launcher.ui.overlay

import android.content.Context
import android.media.AudioManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.yukuza.launcher.data.remote.GeocodingResult
import com.yukuza.launcher.ui.components.glass.GlassCard

@Composable
fun QuickSettingsOverlay(
    onDismiss: () -> Unit,
    isNightMode: Boolean,
    onNightModeToggle: () -> Unit,
    cityQuery: String,
    citySuggestions: List<GeocodingResult>,
    cityName: String,
    onCityQueryChange: (String) -> Unit,
    onCitySelected: (GeocodingResult) -> Unit,
) {
    val context = LocalContext.current

    Popup(
        alignment = Alignment.BottomCenter,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = true),
    ) {
        GlassCard(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 40.dp, vertical = 24.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                // Volume slider
                VolumeSlider(context)

                // Brightness (only if WRITE_SETTINGS granted)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    Settings.System.canWrite(context)
                ) {
                    BrightnessSlider(context)
                }

                // Night Mode toggle
                Row(
                    Modifier
                        .fillMaxWidth()
                        .semantics { contentDescription = "Night Mode" },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Night Mode", color = Color.White, modifier = Modifier.weight(1f))
                    Switch(
                        checked = isNightMode,
                        onCheckedChange = { onNightModeToggle() },
                    )
                }

                // Bluetooth toggle
                BluetoothToggle()

                // Game Mode (only if HDMI_CEC available)
                val hdmiPermission = "android.permission.HDMI_CEC"
                if (context.checkSelfPermission(hdmiPermission) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    GameModeButton()
                }

                // Input Source (only if HDMI_CEC available)
                if (context.checkSelfPermission("android.permission.HDMI_CEC") == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    InputSourceButton()
                }

                // City dropdown
                CityDropdown(
                    cityQuery = cityQuery,
                    suggestions = citySuggestions,
                    cityName = cityName,
                    onQueryChange = onCityQueryChange,
                    onCitySelected = { result ->
                        onCitySelected(result)
                        onDismiss()
                    },
                )
            }
        }
    }
}

@Composable
private fun VolumeSlider(context: Context) {
    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    val maxVolume = remember { audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).toFloat() }
    var volume by remember {
        mutableFloatStateOf(audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat())
    }
    Column {
        Text("Volume", color = Color.White.copy(alpha = 0.7f))
        Slider(
            value = volume,
            onValueChange = { v ->
                volume = v
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, v.toInt(), 0)
            },
            valueRange = 0f..maxVolume,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BrightnessSlider(context: Context) {
    var brightness by remember {
        mutableFloatStateOf(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.SCREEN_BRIGHTNESS,
                    128
                ).toFloat()
            } else 128f
        )
    }
    Column {
        Text("Brightness", color = Color.White.copy(alpha = 0.7f))
        Slider(
            value = brightness,
            onValueChange = { b ->
                brightness = b
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&
                    Settings.System.canWrite(context)
                ) {
                    Settings.System.putInt(
                        context.contentResolver,
                        Settings.System.SCREEN_BRIGHTNESS,
                        b.toInt(),
                    )
                }
            },
            valueRange = 0f..255f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun BluetoothToggle() {
    val btAdapter = remember {
        try {
            android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        } catch (e: Exception) {
            null
        }
    }
    if (btAdapter != null) {
        var btEnabled by remember { mutableStateOf(btAdapter.isEnabled) }
        Row(
            Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Bluetooth" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Bluetooth", color = Color.White, modifier = Modifier.weight(1f))
            Switch(
                checked = btEnabled,
                onCheckedChange = { enabled ->
                    if (enabled) {
                        @Suppress("DEPRECATION")
                        btAdapter.enable()
                    } else {
                        @Suppress("DEPRECATION")
                        btAdapter.disable()
                    }
                    btEnabled = enabled
                },
            )
        }
    }
}

@Composable
private fun GameModeButton() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Game Mode", color = Color.White, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text("HDMI-CEC required", color = Color.White.copy(alpha = 0.4f))
    }
}

@Composable
private fun InputSourceButton() {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Input Source", color = Color.White, modifier = Modifier.weight(1f))
        Spacer(Modifier.width(8.dp))
        Text("HDMI-CEC required", color = Color.White.copy(alpha = 0.4f))
    }
}

@Composable
private fun CityDropdown(
    cityQuery: String,
    suggestions: List<GeocodingResult>,
    cityName: String,
    onQueryChange: (String) -> Unit,
    onCitySelected: (GeocodingResult) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column {
        // Dropdown trigger row
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.2f),
                    shape = RoundedCornerShape(8.dp),
                )
                .clickable { expanded = !expanded }
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (cityName.isNotBlank()) cityName else "Select city…",
                color = if (cityName.isNotBlank()) Color.White else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
            )
        }

        // Expanded: search field + results
        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OutlinedTextField(
                    value = cityQuery,
                    onValueChange = onQueryChange,
                    placeholder = { Text("Search city…", color = Color.White.copy(0.4f)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White.copy(0.6f),
                        unfocusedBorderColor = Color.White.copy(0.2f),
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )

                if (suggestions.isNotEmpty()) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 200.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                    ) {
                        items(suggestions) { result ->
                            CityResultRow(
                                result = result,
                                onClick = {
                                    onCitySelected(result)
                                    expanded = false
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CityResultRow(result: GeocodingResult, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val subtitle = listOfNotNull(result.admin1, result.country).joinToString(", ")

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(
                color = if (isFocused) Color.White.copy(alpha = 0.18f) else Color.Transparent,
                shape = RoundedCornerShape(6.dp),
            )
            .then(
                if (isFocused) Modifier.border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(6.dp),
                ) else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .focusable()
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Column {
            Text(
                text = result.name,
                color = if (isFocused) Color.White else Color.White.copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodyMedium,
            )
            if (subtitle.isNotBlank()) {
                Text(
                    text = subtitle,
                    color = if (isFocused) Color.White.copy(alpha = 0.7f) else Color.White.copy(alpha = 0.4f),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 4.dp),
        color = Color.White.copy(alpha = 0.06f),
    )
}
