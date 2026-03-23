package com.yukuza.launcher.ui.screen.home

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.yukuza.launcher.domain.model.AppInfo
import com.yukuza.launcher.ui.components.AppRow
import com.yukuza.launcher.ui.components.aurora.AuroraBackground
import com.yukuza.launcher.ui.components.widgets.AqiWidget
import com.yukuza.launcher.ui.components.widgets.ClockWidget
import com.yukuza.launcher.ui.components.widgets.NetworkWidget
import com.yukuza.launcher.ui.components.widgets.NowPlayingWidget
import com.yukuza.launcher.ui.components.widgets.ScreenTimerWidget
import com.yukuza.launcher.ui.components.widgets.WeatherWidget
import com.yukuza.launcher.ui.overlay.AppContextMenuOverlay
import com.yukuza.launcher.ui.overlay.CityPickerPopup

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAppFocused: (Int) -> Unit,
    onAppLaunched: (String) -> Unit = {},
    onAppLongPress: (AppInfo) -> Unit = {},
    onReorder: (List<String>) -> Unit,
    onHide: (String) -> Unit = {},
    onUnhide: (String) -> Unit = {},
    onEnterEditMode: () -> Unit = {},
    onRefresh: () -> Unit = {},
    onAssistantClick: () -> Unit,
    onNetworkClick: () -> Unit,
    onSettingsToggle: () -> Unit = {},
    onSeeAllApps: () -> Unit = {},
    onCityQueryChange: (String) -> Unit = {},
    onCitySelected: (com.yukuza.launcher.data.remote.GeocodingResult) -> Unit = {},
    onWeatherClick: () -> Unit = {},
    onNightModeToggle: () -> Unit = {},
    onCheckForUpdate: () -> Unit = {},
    onDismissUpdate: () -> Unit = {},
    onClearUpToDateFlag: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current.density
    var selectedApp by remember { mutableStateOf<AppInfo?>(null) }

    // Refresh on resume (e.g. after disabling an app in Settings)
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) onRefresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Uninstall launcher (result is handled by AppContextMenuOverlay internally,
    // but we declare it here so it's available in the composable scope)
    rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        selectedApp = null
    }

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
                    .padding(top = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ClockWidget()
                Spacer(Modifier.weight(1f))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
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
                    .padding(horizontal = 40.dp, vertical = 20.dp),
            ) {
                AppRow(
                    apps = uiState.apps,
                    focusedIndex = uiState.focusedAppIndex,
                    isEditMode = uiState.isEditMode,
                    onFocus = onAppFocused,
                    onLaunch = onAppLaunched,
                    onReorder = onReorder,
                    onLongPress = { app -> selectedApp = app },
                )
            }
        }

        // Context menu overlay (long-press on home row)
        selectedApp?.let { app ->
            AppContextMenuOverlay(
                app = app,
                onDismiss = { selectedApp = null },
                onHide = { onHide(app.packageName) },
                onUnhide = { onUnhide(app.packageName) },
                onEnterEditMode = {
                    onEnterEditMode()
                    selectedApp = null
                },
            )
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

        // Quick settings overlay
        if (uiState.showSettings) {
            com.yukuza.launcher.ui.overlay.QuickSettingsOverlay(
                onDismiss = onSettingsToggle,
                isNightMode = uiState.isNightMode,
                onNightModeToggle = onNightModeToggle,
                cityQuery = uiState.cityQuery,
                citySuggestions = uiState.citySuggestions,
                cityName = uiState.cityName,
                onCityQueryChange = onCityQueryChange,
                onCitySelected = onCitySelected,
                isCheckingUpdate = uiState.isCheckingUpdate,
                updateInfo = uiState.updateInfo,
                lastCheckWasUpToDate = uiState.lastCheckWasUpToDate,
                onCheckForUpdate = onCheckForUpdate,
                onClearUpToDateFlag = onClearUpToDateFlag,
            )
        }

        // Update dialog
        if (uiState.updateInfo != null) {
            com.yukuza.launcher.ui.overlay.UpdateDialog(
                updateInfo = uiState.updateInfo,
                onDismiss = onDismissUpdate,
            )
        }
    }
}
