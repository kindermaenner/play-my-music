package de.kindermaenner.playmymusic.data.mapping

import com.google.gson.Gson
import de.kindermaenner.playmymusic.data.model.MappingFile
import de.kindermaenner.playmymusic.data.model.Song
import de.kindermaenner.playmymusic.domain.model.Track
import de.kindermaenner.playmymusic.domain.repository.MappingRepository

class JsonMappingRepository : MappingRepository {

    private var songCache: Map<SongKey, Track> = emptyMap()

    fun load(jsonFiles: List<String>) {
        val gson = Gson()
        val allSongs = mutableMapOf<SongKey, Track>()

        for (json in jsonFiles) {
            val mapping = gson.fromJson(json, MappingFile::class.java)

            val edition = mapping.meta.edition
            val editionAbbreviation = mapping.meta.kuerzel
            val urlPart = mapping.meta.urlpart
            val image = mapping.meta.image

            mapping.titles.forEach { song ->
                val key = SongKey(urlPart = urlPart, id = song.id)

                song.edition = edition
                song.kuerzel = editionAbbreviation
                song.image = image
                song.urlpart = urlPart

                allSongs[key] = song.toDomain()
            }
        }
        songCache = allSongs
    }

    override fun getTrack(urlPart: String, id: Int): Track? {
        return songCache[SongKey(urlPart, id)]
    }

    private data class SongKey(
        val urlPart: String,
        val id: Int
    )
}
