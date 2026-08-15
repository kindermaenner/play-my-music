package de.kindermaenner.playmymusic.domain.repository

import de.kindermaenner.playmymusic.domain.model.Track

interface MappingRepository {
    fun getTrack(urlPart: String, id: Int): Track?
}
