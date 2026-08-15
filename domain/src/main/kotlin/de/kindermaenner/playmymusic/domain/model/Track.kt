package de.kindermaenner.playmymusic.domain.model

data class Track(
    val id: Int,
    val title: String,
    val artist: String,
    val year: Int,
    val edition: String,
    val editionAbbreviation: String,
    val urlPart: String,
    val image: String,
    val nasLocation: String
)
