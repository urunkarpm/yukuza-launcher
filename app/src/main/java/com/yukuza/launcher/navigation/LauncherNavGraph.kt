package com.yukuza.launcher.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yukuza.launcher.ui.screen.home.HomeScreen
import com.yukuza.launcher.ui.screen.home.HomeViewModel

@Composable
fun LauncherNavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "home") {
        composable("home") {
            val vm: HomeViewModel = hiltViewModel()
            val state by vm.uiState.collectAsStateWithLifecycle()
            HomeScreen(
                uiState = state,
                onAppFocused = vm::onAppFocused,
                onAppLaunched = vm::onAppLaunched,
                onAppLongPress = { /* App shortcuts overlay — Task 18 */ },
                onReorder = vm::reorder,
                onAssistantClick = { },
                onNetworkClick = { },
                onSettingsToggle = vm::toggleSettings,
                onSeeAllApps = { navController.navigate("apps") },
                onCityQueryChange = vm::onCityQueryChange,
                onCitySelected = vm::onCitySelected,
                onWeatherClick = vm::toggleCityPicker,
            )
        }
        composable("apps") {
            // AppListScreen added in Task 17
            androidx.compose.material3.Text(androidx.compose.ui.res.stringResource(com.yukuza.launcher.R.string.app_list_coming_soon))
        }
    }
}
