package com.yukuza.launcher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yukuza.launcher.ui.components.AssistantButton
import com.yukuza.launcher.ui.theme.YukuzaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AssistantButtonTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun assistantButton_isDisplayed() {
        composeRule.setContent {
            YukuzaTheme {
                AssistantButton()
            }
        }
        composeRule.onNodeWithContentDescription("Google Assistant").assertIsDisplayed()
    }
}
