package de.kindermaenner.playmymusic.core.mapping

import de.kindermaenner.playmymusic.core.model.PlaybackSettings
import de.kindermaenner.playmymusic.core.model.Song
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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

    @Test
    fun `resolve handles smb urls with forward slashes`() {
        val song = Song(
            id = 1,
            title = "Song",
            artist = "Artist",
            year = 2024,
            nasLocation = "",
            edition = "Rock"
        )

        val result = TrackPathResolver.resolve(
            settings = PlaybackSettings(
                nasAddress = "smb://my-nas",
                nasBaseFolder = "music"
            ),
            song = song,
            trackIdPadding = 3,
            trackFolderName = "tracks"
        )

        assertEquals("smb://my-nas/music/Rock/tracks/001.mp3", result)
    }

    @Test
    fun `resolve returns null for invalid address or missing folder`() {
        val song = Song(1, "T", "A", 0, "", edition = "E")
        
        assertNull(TrackPathResolver.resolve(PlaybackSettings("", "B"), song))
        assertNull(TrackPathResolver.resolve(PlaybackSettings("A", ""), song))
        assertNull(TrackPathResolver.resolve(PlaybackSettings("A", "B"), song.copy(edition = "")))
    }

    @Test
    fun `resolve uses explicit nasLocation even with smb url`() {
        val song = Song(1, "T", "A", 0, "explicit/path.mp3")
        val result = TrackPathResolver.resolve(
            settings = PlaybackSettings(nasAddress = "smb://nas", nasBaseFolder = "ignored"),
            song = song
        )
        assertEquals("smb://nas/explicit/path.mp3", result)
    }
}
