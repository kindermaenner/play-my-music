package de.kindermaenner.playmymusic

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.ui.main.MainScreen

import de.kindermaenner.playmymusic.ui.main.MainUiState

@Composable
fun App(
    debugMode: Boolean,
    appSettings: AppSettings,
    onOpenSettings: () -> Unit
){
    Box(Modifier.fillMaxSize()) {
        MainScreen(
            uiState = MainUiState(),
            debugMode = debugMode,
            appSettings = appSettings,
            onQrScanned = {},
            onPlayPauseToggled = {},
            onPlaybackError = {},
            onResetToScanner = {},
            onOpenSettings = onOpenSettings,
            onLoadMappings = {}
        )
    }
}
