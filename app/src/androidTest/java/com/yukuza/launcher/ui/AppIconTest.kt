package com.yukuza.launcher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yukuza.launcher.domain.model.AppInfo
import com.yukuza.launcher.ui.components.AppIcon
import com.yukuza.launcher.ui.theme.YukuzaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppIconTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appIcon_showsLabel() {
        val app = AppInfo("com.example.test", "Test App", 0)
        composeRule.setContent {
            YukuzaTheme {
                AppIcon(
                    app = app,
                    isFocused = false,
                    onFocus = {},
                    onLongPress = {},
                )
            }
        }
        composeRule.onNodeWithText("Test App").assertIsDisplayed()
    }
}
