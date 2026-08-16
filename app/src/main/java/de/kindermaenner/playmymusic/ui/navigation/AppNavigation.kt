package de.kindermaenner.playmymusic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.ui.main.MainScreen
import de.kindermaenner.playmymusic.ui.main.MainUiState
import de.kindermaenner.playmymusic.ui.settings.SettingsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    uiState: MainUiState,
    debugMode: Boolean,
    appSettings: AppSettings,
    syncMessage: String?,
    isSyncing: Boolean,
    isCameraAvailable: Boolean,
    cameraPermissionGranted: Boolean,
    onQrScanned: (String) -> Unit,
    onPlayPauseToggled: (Boolean) -> Unit,
    onPlaybackError: (String) -> Unit,
    onResetToScanner: () -> Unit,
    onLoadMappings: () -> Unit,
    onOpenSettings: () -> Unit,
    onDebugToggle: (Boolean) -> Unit,
    onSettingsSave: (AppSettings) -> Unit,
    onSyncMetadata: () -> Unit,
    onSyncTracks: () -> Unit,
    onRequestCameraPermission: () -> Unit
) {
    NavHost(navController, startDestination = "main") {

        composable("main") {
            MainScreen(
                uiState = uiState,
                debugMode = debugMode,
                appSettings = appSettings,
                isCameraAvailable = isCameraAvailable,
                cameraPermissionGranted = cameraPermissionGranted,
                onQrScanned = onQrScanned,
                onPlayPauseToggled = onPlayPauseToggled,
                onPlaybackError = onPlaybackError,
                onResetToScanner = onResetToScanner,
                onLoadMappings = onLoadMappings,
                onOpenSettings = onOpenSettings,
                onRequestCameraPermission = onRequestCameraPermission
            )
        }

        composable("settings") {
            SettingsScreen(
                debugMode = debugMode,
                appSettings = appSettings,
                syncMessage = syncMessage,
                isSyncing = isSyncing,
                onDebugToggle = onDebugToggle,
                onSave = { settings ->
                    onSettingsSave(settings)
                    navController.popBackStack()
                },
                onSyncMetadata = onSyncMetadata,
                onSyncTracks = onSyncTracks,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
