package de.kindermaenner.playmymusic.core.mapping

import de.kindermaenner.playmymusic.core.model.MappingFile
import de.kindermaenner.playmymusic.core.model.Song
import de.kindermaenner.playmymusic.core.model.SongKey
import com.google.gson.Gson

object MappingLoader {

    private var cache: Map<SongKey, Song>? = null
    private var cacheInput: List<String>? = null

    fun parse(jsonFiles: List<String>): Map<SongKey, Song> {
        if (cacheInput == jsonFiles) {
            cache?.let { return it }
        }

        val gson = Gson()
        val allSongs = mutableMapOf<SongKey, Song>()

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

                allSongs[key] = song
            }
        }

        cacheInput = jsonFiles.toList()
        cache = allSongs
        return allSongs
    }
}
