package com.yukuza.launcher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yukuza.launcher.ui.screen.home.HomeScreen
import com.yukuza.launcher.ui.screen.home.HomeUiState
import com.yukuza.launcher.ui.theme.YukuzaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class HomeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun homeScreen_rendersWithoutCrash() {
        composeRule.setContent {
            YukuzaTheme {
                HomeScreen(
                    uiState = HomeUiState(),
                    onAppFocused = {},
                    onAppLongPress = {},
                    onReorder = {},
                    onAssistantClick = {},
                    onNetworkClick = {},
                )
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }
}
