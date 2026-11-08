package de.kindermaenner.playmymusic.ui.main

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import de.kindermaenner.playmymusic.domain.model.Track
import de.kindermaenner.playmymusic.settings.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w400dp-h900dp-xxhdpi")
class MainScreenFlowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleTrack(id: Int = 42) = Track(
        id = id,
        title = "Bohemian Rhapsody",
        artist = "Queen",
        year = 1975,
        edition = "Rock Edition",
        editionAbbreviation = "ROC",
        urlPart = "p1",
        image = "",
        nasLocation = ""
    )

    @Test
    fun cameraHardwareMissing_displaysNoCameraHardwareMessage() {
        composeTestRule.setContent {
            MaterialTheme {
                MainScreen(
                    uiState = MainUiState(isScanning = true),
                    debugMode = false,
                    appSettings = AppSettings(),
                    isCameraAvailable = false,
                    cameraPermissionGranted = true,
                    onQrScanned = {},
                    onPlayPauseToggled = {},
                    onPlaybackError = {},
                    onResetToScanner = {},
                    onLoadMappings = {},
                    onOpenSettings = {},
                    onRequestCameraPermission = {}
                )
            }
        }

        composeTestRule.onNodeWithText("No camera hardware detected.\nCannot scan QR codes.")
            .assertIsDisplayed()
    }

    @Test
    fun cameraPermissionNotGranted_displaysPermissionRequiredAndInvokesCallback() {
        var requested = false

        composeTestRule.setContent {
            MaterialTheme {
                MainScreen(
                    uiState = MainUiState(isScanning = true),
                    debugMode = false,
                    appSettings = AppSettings(),
                    isCameraAvailable = true,
                    cameraPermissionGranted = false,
                    onQrScanned = {},
                    onPlayPauseToggled = {},
                    onPlaybackError = {},
                    onResetToScanner = {},
                    onLoadMappings = {},
                    onOpenSettings = {},
                    onRequestCameraPermission = { requested = true }
                )
            }
        }

        composeTestRule.onNodeWithText("Camera permission required.").assertIsDisplayed()
        composeTestRule.onNodeWithText("Grant Permission").performClick()
        assertTrue(requested)
    }

    @Test
    fun trackLoaded_displaysTrackDetails_andNextCardInvokesReset() {
        var resetCalled = false

        composeTestRule.setContent {
            MaterialTheme {
                MainScreen(
                    uiState = MainUiState(
                        isScanning = false,
                        currentTrack = sampleTrack(42),
                        isPlaying = true
                    ),
                    debugMode = false,
                    appSettings = AppSettings(),
                    isCameraAvailable = true,
                    cameraPermissionGranted = true,
                    onQrScanned = {},
                    onPlayPauseToggled = {},
                    onPlaybackError = {},
                    onResetToScanner = { resetCalled = true },
                    onLoadMappings = {},
                    onOpenSettings = {},
                    onRequestCameraPermission = {}
                )
            }
        }

        composeTestRule.onNodeWithText("ROC").assertIsDisplayed()
        composeTestRule.onNodeWithText("42").assertIsDisplayed()

        composeTestRule.onNodeWithText("next card").performClick()
        assertTrue(resetCalled)
    }

    @Test
    fun errorState_displaysErrorMessage() {
        composeTestRule.setContent {
            MaterialTheme {
                MainScreen(
                    uiState = MainUiState(
                        isScanning = false,
                        error = "Fehler bei Wiedergabe"
                    ),
                    debugMode = false,
                    appSettings = AppSettings(),
                    isCameraAvailable = true,
                    cameraPermissionGranted = true,
                    onQrScanned = {},
                    onPlayPauseToggled = {},
                    onPlaybackError = {},
                    onResetToScanner = {},
                    onLoadMappings = {},
                    onOpenSettings = {},
                    onRequestCameraPermission = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Fehler bei Wiedergabe").assertExists()
    }

    @Test
    fun debugMode_displaysTrackDebugDetails() {
        composeTestRule.setContent {
            MaterialTheme {
                MainScreen(
                    uiState = MainUiState(
                        isScanning = false,
                        currentTrack = sampleTrack(42),
                        playbackUri = "smb://192.168.1.1/music/0042.mp3"
                    ),
                    debugMode = true,
                    appSettings = AppSettings(),
                    isCameraAvailable = true,
                    cameraPermissionGranted = true,
                    onQrScanned = {},
                    onPlayPauseToggled = {},
                    onPlaybackError = {},
                    onResetToScanner = {},
                    onLoadMappings = {},
                    onOpenSettings = {},
                    onRequestCameraPermission = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Track Info:").assertExists()
        composeTestRule.onNodeWithText("- Title: Bohemian Rhapsody").assertExists()
        composeTestRule.onNodeWithText("- Artist: Queen").assertExists()
        composeTestRule.onNodeWithText("- Year: 1975").assertExists()
        composeTestRule.onNodeWithText("- URI: smb://192.168.1.1/music/0042.mp3").assertExists()
    }

    @Test
    fun offlineOnboardingHint_dismissHidesHint() {
        composeTestRule.setContent {
            MaterialTheme {
                MainScreen(
                    uiState = MainUiState(isScanning = true, currentTrack = null),
                    debugMode = false,
                    appSettings = AppSettings(),
                    isCameraAvailable = true,
                    cameraPermissionGranted = true,
                    onQrScanned = {},
                    onPlayPauseToggled = {},
                    onPlaybackError = {},
                    onResetToScanner = {},
                    onLoadMappings = {},
                    onOpenSettings = {},
                    onRequestCameraPermission = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("offline_onboarding_hint").assertExists()
        composeTestRule.onNodeWithText("OK").performClick()
        composeTestRule.onNodeWithTag("offline_onboarding_hint").assertDoesNotExist()
    }

    @Test
    fun settingsButtonClick_invokesOpenSettings() {
        var settingsOpened = false

        composeTestRule.setContent {
            MaterialTheme {
                MainScreen(
                    uiState = MainUiState(isScanning = true),
                    debugMode = false,
                    appSettings = AppSettings(),
                    isCameraAvailable = true,
                    cameraPermissionGranted = true,
                    onQrScanned = {},
                    onPlayPauseToggled = {},
                    onPlaybackError = {},
                    onResetToScanner = {},
                    onLoadMappings = {},
                    onOpenSettings = { settingsOpened = true },
                    onRequestCameraPermission = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Play My Music!").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Settings").performClick()
        assertTrue(settingsOpened)
    }
}
