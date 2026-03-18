package com.yukuza.launcher.ui

import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yukuza.launcher.ui.components.glass.GlassCard
import com.yukuza.launcher.ui.theme.YukuzaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GlassCardTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun glassCard_displaysContent() {
        composeRule.setContent {
            YukuzaTheme {
                GlassCard {
                    Text("Hello", modifier = Modifier.testTag("content"))
                }
            }
        }
        composeRule.onNodeWithTag("content").assertIsDisplayed()
    }
}
