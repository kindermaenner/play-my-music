package de.kindermaenner.playmymusic.domain.model

data class PlaybackConfig(
    val nasAddress: String,
    val nasBaseFolder: String,
    val trackIdPadding: Int = 4,
    val trackFolderName: String = "tracks"
)
