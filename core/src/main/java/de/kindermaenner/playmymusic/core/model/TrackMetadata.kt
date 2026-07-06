package de.kindermaenner.playmymusic.core.model

data class MappingFile(
    val meta: MetaData,
    val titles: List<Song>
)

data class MetaData(
    val edition: String,
    val kuerzel: String,
    val urlpart: String,
    val image: String
)

data class Song(
    val id: Int,
    val title: String,
    val artist: String,
    val year: Int,
    val nasLocation: String,

    // from meta data
    var edition: String? = null,
    var kuerzel: String? = null,
    var urlpart: String? = null,
    var image: String? = null
)

data class SongKey(
    val urlPart: String,
    val id: Int
)

data class PlaybackSettings(
    val nasAddress: String,
    val nasBaseFolder: String
)
