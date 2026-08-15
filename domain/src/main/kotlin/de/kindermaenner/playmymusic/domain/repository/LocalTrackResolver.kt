package de.kindermaenner.playmymusic.domain.repository

import de.kindermaenner.playmymusic.domain.model.Track

interface LocalTrackResolver {
    fun resolveLocalUri(track: Track): String?
}
