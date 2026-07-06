package de.kindermaenner.playmymusic.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.ui.main.MainScreen
import de.kindermaenner.playmymusic.ui.settings.SettingsScreen

@Composable
fun AppNavigation(
    navController: NavHostController,
    debugMode: Boolean,
    appSettings: AppSettings,
    syncMessage: String?,
    isSyncing: Boolean,
    onOpenSettings: () -> Unit,
    onDebugToggle: (Boolean) -> Unit,
    onSettingsSave: (AppSettings) -> Unit,
    onSyncMetadata: () -> Unit,
    onSyncTracks: () -> Unit
) {
    NavHost(navController, startDestination = "main") {

        composable("main") {
            MainScreen(
                debugMode = debugMode,
                appSettings = appSettings,
                onOpenSettings = onOpenSettings
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
