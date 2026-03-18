package com.yukuza.launcher.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yukuza.launcher.ui.components.aurora.AuroraBackground
import com.yukuza.launcher.ui.theme.YukuzaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AuroraBackgroundTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun auroraBackground_rendersWithoutCrash() {
        composeRule.setContent {
            YukuzaTheme {
                Box(Modifier.fillMaxSize()) {
                    AuroraBackground()
                }
            }
        }
        composeRule.onRoot().assertIsDisplayed()
    }
}
