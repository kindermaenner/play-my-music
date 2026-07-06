package de.kindermaenner.playmymusic.ui.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.swipeUp
import de.kindermaenner.playmymusic.core.model.PlaybackSettings
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SmbAuthSettings
import de.kindermaenner.playmymusic.ui.state.logVisibleStates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test

class SettingsScreenFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun saveFlow_emitsEditedSettings() {
        var savedSettings: AppSettings? = null

        composeTestRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    debugMode = false,
                    appSettings = sampleSettings(),
                    syncMessage = null,
                    isSyncing = false,
                    onDebugToggle = {},
                    onSave = { savedSettings = it },
                    onSyncMetadata = {},
                    onSyncTracks = {},
                    onBack = {}
                )
            }
        }

        composeTestRule.logVisibleStates()

        composeTestRule.onNodeWithTag("nas_address_field").performTextClearance()
        composeTestRule.onNodeWithTag("nas_address_field").performTextInput("10.0.0.8")
        composeTestRule.onNodeWithTag("nas_base_folder_field").performTextClearance()
        composeTestRule.onNodeWithTag("nas_base_folder_field").performTextInput("Media\\Party")
        composeTestRule.onNodeWithTag("qr_validation_field").performTextClearance()
        composeTestRule.onNodeWithTag("qr_validation_field").performTextInput("example.org/code/")
        composeTestRule.onNodeWithTag("mapping_file_postfix_field").performTextClearance()
        composeTestRule.onNodeWithTag("mapping_file_postfix_field").performTextInput("rock")
        composeTestRule.onNodeWithTag("track_id_padding_field").performTextClearance()
        composeTestRule.onNodeWithTag("track_id_padding_field").performTextInput("5")
        composeTestRule.onNodeWithTag("track_folder_name_field").performTextClearance()
        composeTestRule.onNodeWithTag("track_folder_name_field").performTextInput("audio")
        composeTestRule.onNodeWithTag("smb_username_field").performTextClearance()
        composeTestRule.onNodeWithTag("smb_username_field").performTextInput("guestuser")
        composeTestRule.onNodeWithTag("smb_password_field").performTextClearance()
        composeTestRule.onNodeWithTag("smb_password_field").performTextInput("secret")
        composeTestRule.onNodeWithTag("smb_domain_field").performTextClearance()
        composeTestRule.onNodeWithTag("smb_domain_field").performTextInput("WORKGROUP")

        scrollToBottom()
        composeTestRule.onNodeWithTag("save_button").performClick()
        composeTestRule.logVisibleStates()

        composeTestRule.runOnIdle {
            val result = savedSettings
            assertNotNull(result)
            assertEquals("10.0.0.8", result!!.playback.nasAddress)
            assertEquals("Media\\Party", result.playback.nasBaseFolder)
            assertEquals("example.org/code/", result.qrValidation)
            assertEquals("rock", result.mappingFilePostfix)
            assertEquals(5, result.trackIdPadding)
            assertEquals("audio", result.trackFolderName)
            assertEquals("guestuser", result.smbAuth.username)
            assertEquals("secret", result.smbAuth.password)
            assertEquals("WORKGROUP", result.smbAuth.domain)
        }
    }

    @Test
    fun syncFlow_invokesActions_andDisplaysMessage() {
        var metadataSyncCalls = 0
        var trackSyncCalls = 0

        composeTestRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    debugMode = false,
                    appSettings = sampleSettings(),
                    syncMessage = "Metadata synced successfully",
                    isSyncing = false,
                    onDebugToggle = {},
                    onSave = {},
                    onSyncMetadata = { metadataSyncCalls += 1 },
                    onSyncTracks = { trackSyncCalls += 1 },
                    onBack = {}
                )
            }
        }

        composeTestRule.logVisibleStates()

        composeTestRule.runOnIdle {
            val messageNodes = composeTestRule
                .onAllNodesWithTag("sync_message_text", useUnmergedTree = true)
                .fetchSemanticsNodes()
            assertEquals(1, messageNodes.size)
        }
        composeTestRule.onNodeWithText("Metadata synced successfully").fetchSemanticsNode()

        scrollToBottom()
        composeTestRule.onNodeWithTag("sync_metadata_button").performClick()
        composeTestRule.logVisibleStates()
        composeTestRule.onNodeWithTag("sync_tracks_button").performClick()
        composeTestRule.logVisibleStates()

        composeTestRule.runOnIdle {
            assertEquals(1, metadataSyncCalls)
            assertEquals(1, trackSyncCalls)
        }
    }

    @Test
    fun syncingState_disablesSyncButtons_andBackRemainsAvailable() {
        var backCalls = 0

        composeTestRule.setContent {
            MaterialTheme {
                SettingsScreen(
                    debugMode = true,
                    appSettings = sampleSettings(),
                    syncMessage = null,
                    isSyncing = true,
                    onDebugToggle = {},
                    onSave = {},
                    onSyncMetadata = {},
                    onSyncTracks = {},
                    onBack = { backCalls += 1 }
                )
            }
        }

        composeTestRule.logVisibleStates()

        composeTestRule.runOnIdle {
            val messageNodes = composeTestRule
                .onAllNodesWithTag("sync_message_text", useUnmergedTree = true)
                .fetchSemanticsNodes()
            assertEquals(0, messageNodes.size)
        }
        scrollToBottom()
        composeTestRule.onNodeWithTag("sync_metadata_button").assertIsNotEnabled()
        composeTestRule.onNodeWithTag("sync_tracks_button").assertIsNotEnabled()

        scrollToBottom()
        composeTestRule.onNodeWithTag("back_button").performClick()
        composeTestRule.logVisibleStates()

        composeTestRule.runOnIdle {
            assertEquals(1, backCalls)
        }
    }

    private fun scrollToBottom() {
        repeat(3) {
            composeTestRule.onNodeWithTag("settings_screen_scroll")
                .performTouchInput { swipeUp() }
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
            smbAuth = SmbAuthSettings(
                username = "",
                password = "",
                domain = ""
            )
        )
    }
}