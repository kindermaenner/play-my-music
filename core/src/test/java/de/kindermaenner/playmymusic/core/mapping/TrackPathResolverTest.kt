package de.kindermaenner.playmymusic.core.mapping

import de.kindermaenner.playmymusic.core.model.PlaybackSettings
import de.kindermaenner.playmymusic.core.model.Song
import org.junit.Assert.assertEquals
import org.junit.Test

class TrackPathResolverTest {

    @Test
    fun `resolve builds default nas path from edition and padded file name`() {
        val song = Song(
            id = 23,
            title = "Titel",
            artist = "Artist",
            year = 1990,
            nasLocation = "",
            edition = "FunnyOne"
        )

        val result = TrackPathResolver.resolve(
            settings = PlaybackSettings(
                nasAddress = "NAS01",
                nasBaseFolder = "Multimedia\\Tracks"
            ),
            song = song,
            trackFolderName = "tracks"
        )

        assertEquals("\\\\NAS01\\Multimedia\\Tracks\\FunnyOne\\tracks\\0023.mp3", result)
    }

    @Test
    fun `resolve prefers explicit nas location when present`() {
        val song = Song(
            id = 99,
            title = "Titel",
            artist = "Artist",
            year = 1990,
            nasLocation = "Sonderordner\\Datei.mp3"
        )

        val result = TrackPathResolver.resolve(
            settings = PlaybackSettings(
                nasAddress = "NAS01",
                nasBaseFolder = "Multimedia"
            ),
            song = song
        )

        assertEquals("\\\\NAS01\\Sonderordner\\Datei.mp3", result)
    }
}