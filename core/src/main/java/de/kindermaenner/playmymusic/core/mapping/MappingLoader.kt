package de.kindermaenner.playmymusic.core.mapping

import de.kindermaenner.playmymusic.core.model.Song
import de.kindermaenner.playmymusic.core.model.MappingFile
import com.google.gson.Gson

object MappingLoader {

    private var cache: Map<String, Song>? = null

    fun parse(jsonFiles: List<String>): Map<String, Song> {
        cache?.let { return it }

        val gson = Gson()
        val allSongs = mutableMapOf<String, Song>()

        for (json in jsonFiles) {
            val mapping = gson.fromJson(json, MappingFile::class.java)

            val edition = mapping.meta.edition
            val editionAbbreviation = mapping.meta.kuerzel
            val urlPart = mapping.meta.urlpart
            val image = mapping.meta.image

            mapping.titles.forEach { song ->
                val paddedId = "%05d".format(song.id)

                val key = if (urlPart.isBlank()) paddedId else "$urlPart/$paddedId"

                song.edition = edition
                song.kuerzel = editionAbbreviation
                song.image = image
                song.urlpart = urlPart

                allSongs[key] = song
            }
        }

        cache = allSongs
        return allSongs
    }
}
