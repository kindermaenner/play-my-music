package de.kindermaenner.playmymusic.data.mapping

import de.kindermaenner.playmymusic.data.model.Song
import de.kindermaenner.playmymusic.domain.model.Track

fun Song.toDomain(): Track {
    return Track(
        id = this.id,
        title = this.title,
        artist = this.artist,
        year = this.year,
        edition = this.edition.orEmpty(),
        editionAbbreviation = this.kuerzel.orEmpty(),
        urlPart = this.urlpart.orEmpty(),
        image = this.image.orEmpty(),
        nasLocation = this.nasLocation
    )
}
