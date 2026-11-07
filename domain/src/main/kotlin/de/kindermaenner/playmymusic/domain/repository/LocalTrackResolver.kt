package de.kindermaenner.playmymusic.domain.repository

import de.kindermaenner.playmymusic.domain.model.Track

fun interface LocalTrackResolver {
    fun resolveLocalUri(track: Track): String?
}
