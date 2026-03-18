package com.yukuza.launcher.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.yukuza.launcher.domain.model.MediaData
import com.yukuza.launcher.ui.components.widgets.NowPlayingWidget
import com.yukuza.launcher.ui.theme.YukuzaTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NowPlayingWidgetTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun nowPlayingWidget_hiddenWhenDataIsNull() {
        composeRule.setContent {
            YukuzaTheme {
                NowPlayingWidget(data = null)
            }
        }
        composeRule.onNodeWithTag("now_playing").assertDoesNotExist()
    }

    @Test
    fun nowPlayingWidget_showsTrackTitleWhenPlaying() {
        val data = MediaData(
            trackTitle = "Blinding Lights",
            artist = "The Weeknd",
            albumArtUri = null,
            elapsedMs = 84_000,
            durationMs = 200_000,
            isPlaying = true,
            sourceAppLabel = "Spotify",
        )
        composeRule.setContent {
            YukuzaTheme {
                NowPlayingWidget(data = data)
            }
        }
        composeRule.onNodeWithText("Blinding Lights").assertIsDisplayed()
    }
}
