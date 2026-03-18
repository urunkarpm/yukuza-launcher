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
import com.yukuza.launcher.ui.components.AssistantButton
import com.yukuza.launcher.ui.components.aurora.AuroraBackground
import com.yukuza.launcher.ui.components.widgets.AqiWidget
import com.yukuza.launcher.ui.components.widgets.ClockWidget
import com.yukuza.launcher.ui.components.widgets.NetworkWidget
import com.yukuza.launcher.ui.components.widgets.NowPlayingWidget
import com.yukuza.launcher.ui.components.widgets.ScreenTimerWidget
import com.yukuza.launcher.ui.components.widgets.WeatherWidget

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAppFocused: (Int) -> Unit,
    onAppLongPress: (AppInfo) -> Unit,
    onReorder: (List<String>) -> Unit,
    onAssistantClick: () -> Unit,
    onNetworkClick: () -> Unit,
    onSeeAllApps: () -> Unit = {},
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
            // Top bar — 3 equal-weight columns prevent overlap
            Row(
                Modifier
                    .align(Alignment.TopStart)
                    .fillMaxWidth()
                    .padding(top = 32.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Left: Clock
                Row(
                    Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) { ClockWidget() }

                // Center: Assistant G button
                Box(
                    Modifier.weight(1f),
                    contentAlignment = Alignment.Center,
                ) { AssistantButton() }

                // Right: Weather, AQI, Screen Timer, Network
                Row(
                    Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    uiState.weather?.let { WeatherWidget(it) }
                    uiState.aqi?.let { AqiWidget(it) }
                    ScreenTimerWidget()
                    uiState.network?.let { NetworkWidget(it) }
                }
            }

            // Center: Now Playing widget (only when media active)
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
                    onReorder = onReorder,
                    onLongPress = onAppLongPress,
                )
            }
        }
    }
}
