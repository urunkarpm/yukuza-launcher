package com.yukuza.launcher.ui.screen.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.yukuza.launcher.domain.model.AppInfo
import com.yukuza.launcher.ui.components.AppRow
import com.yukuza.launcher.ui.components.aurora.AuroraBackground
import com.yukuza.launcher.ui.components.widgets.AqiWidget
import com.yukuza.launcher.ui.components.widgets.ClockWidget
import com.yukuza.launcher.ui.components.widgets.NetworkWidget
import com.yukuza.launcher.ui.components.widgets.NowPlayingWidget
import com.yukuza.launcher.ui.components.widgets.ScreenTimerWidget
import com.yukuza.launcher.ui.components.widgets.WeatherWidget
import com.yukuza.launcher.ui.overlay.CityPickerPopup

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAppFocused: (Int) -> Unit,
    onAppLaunched: (String) -> Unit = {},
    onAppLongPress: (AppInfo) -> Unit,
    onReorder: (List<String>) -> Unit,
    onAssistantClick: () -> Unit,
    onNetworkClick: () -> Unit,
    onSettingsToggle: () -> Unit = {},
    onSeeAllApps: () -> Unit = {},
    onCityQueryChange: (String) -> Unit = {},
    onCitySelected: (com.yukuza.launcher.data.remote.GeocodingResult) -> Unit = {},
    onWeatherClick: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density

    Box(modifier = modifier.fillMaxSize()) {
        // Layer 1: Aurora animated background
        AuroraBackground()

        // Layer 2: UI content
        Box(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 40.dp),
        ) {
            // Top bar — clock left, widgets right
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClockWidget()
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    uiState.weather?.let { WeatherWidget(it, onClick = onWeatherClick) }
                    uiState.aqi?.let { AqiWidget(it) }
                    ScreenTimerWidget()
                    uiState.network?.let { NetworkWidget(it) }
                }
            }

            // Center: Now Playing widget
            NowPlayingWidget(
                data = uiState.nowPlaying,
                modifier = Modifier.align(Alignment.Center),
            )

            // Bottom: App strip
            Box(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color(0xD004020C))
                    .border(
                        BorderStroke(
                            width = if (density >= 2f) 0.5.dp else 1.dp,
                            color = Color(0x038632FA).copy(alpha = 0.22f),
                        ),
                        RectangleShape,
                    )
                    .padding(horizontal = 40.dp, vertical = 16.dp),
            ) {
                AppRow(
                    apps = uiState.apps,
                    focusedIndex = uiState.focusedAppIndex,
                    isEditMode = uiState.isEditMode,
                    onFocus = onAppFocused,
                    onLaunch = onAppLaunched,
                    onReorder = onReorder,
                    onLongPress = onAppLongPress,
                )
            }
        }

        // City picker popup (triggered by tapping weather widget)
        if (uiState.showCityPicker) {
            CityPickerPopup(
                cityQuery = uiState.cityQuery,
                citySuggestions = uiState.citySuggestions,
                cityName = uiState.cityName,
                onCityQueryChange = onCityQueryChange,
                onCitySelected = onCitySelected,
                onDismiss = onWeatherClick,
            )
        }
    }
}
