package de.kindermaenner.playmymusic.ui.navigation

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.navigation.compose.rememberNavController
import de.kindermaenner.playmymusic.core.model.PlaybackSettings
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SmbAuthSettings
import de.kindermaenner.playmymusic.ui.state.logVisibleStates
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class AppNavigationFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun navigationFlow_logsMainAndSettingsStates() {
        var saveCalls = 0
        var backSyncMetadataCalls = 0
        var backSyncTracksCalls = 0

        composeTestRule.setContent {
            MaterialTheme {
                AppNavigation(
                    navController = rememberNavController(),
                    debugMode = true,
                    appSettings = sampleSettings(),
                    syncMessage = null,
                    isSyncing = false,
                    onOpenSettings = {},
                    onDebugToggle = {},
                    onSettingsSave = { saveCalls += 1 },
                    onSyncMetadata = { backSyncMetadataCalls += 1 },
                    onSyncTracks = { backSyncTracksCalls += 1 }
                )
            }
        }

        composeTestRule.logVisibleStates()
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        composeTestRule.logVisibleStates()

        repeat(3) {
            composeTestRule.onNodeWithTag("settings_screen_scroll")
                .performTouchInput { swipeUp() }
        }
        composeTestRule.onNodeWithTag("back_button").performClick()
        composeTestRule.logVisibleStates()

        composeTestRule.runOnIdle {
            assertEquals(0, saveCalls)
            assertEquals(0, backSyncMetadataCalls)
            assertEquals(0, backSyncTracksCalls)
        }
    }

    private fun sampleSettings(): AppSettings {
        return AppSettings(
            playback = PlaybackSettings(
                nasAddress = "192.168.0.45",
                nasBaseFolder = "Multimedia\\playmymusic"
            ),
            qrValidation = "example.org/game/de/",
            mappingFilePostfix = "",
            trackIdPadding = 3,
            trackFolderName = "tracks",
            smbAuth = SmbAuthSettings()
        )
    }
}