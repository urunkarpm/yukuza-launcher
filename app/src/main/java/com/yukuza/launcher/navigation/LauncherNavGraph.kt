package com.yukuza.launcher.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.yukuza.launcher.ui.screen.apps.AppListScreen
import com.yukuza.launcher.ui.screen.apps.AppListViewModel
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
                onReorder = vm::reorder,
                onHide = vm::hideApp,
                onUnhide = vm::unhideApp,
                onEnterEditMode = vm::enterEditMode,
                onRefresh = vm::refresh,
                onAssistantClick = { },
                onNetworkClick = { },
                onSettingsToggle = vm::toggleSettings,
                onSeeAllApps = { navController.navigate("apps") },
                onCityQueryChange = vm::onCityQueryChange,
                onCitySelected = vm::onCitySelected,
                onWeatherClick = vm::toggleCityPicker,
                onNightModeToggle = { vm.setNightMode(!state.isNightMode) },
                onCheckForUpdate = vm::checkForUpdate,
                onDismissUpdate = vm::dismissUpdate,
                onClearUpToDateFlag = vm::clearUpToDateFlag,
                onSetWallpaper = vm::setWallpaper,
                onClearWallpaper = vm::clearWallpaper,
            )
        }
        composable("apps") {
            val vm: AppListViewModel = hiltViewModel()
            AppListScreen(
                viewModel = vm,
                onBack = { navController.popBackStack() },
            )
        }
    }
}
