package de.kindermaenner.playmymusic.ui.main

import de.kindermaenner.playmymusic.domain.model.Track

data class MainUiState(
    val isScanning: Boolean = true,
    val currentTrack: Track? = null,
    val isPlaying: Boolean = false,
    val error: String? = null,
    val playbackUri: String? = null,
    val debugInfo: String? = null
)
