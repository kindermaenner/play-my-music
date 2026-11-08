package de.kindermaenner.playmymusic.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TrackMetadataTest {

    @Test
    fun `instantiate and verify PlaybackSettings`() {
        val settings = PlaybackSettings(nasAddress = "192.168.1.10", nasBaseFolder = "music")
        assertEquals("192.168.1.10", settings.nasAddress)
        assertEquals("music", settings.nasBaseFolder)
    }

    @Test
    fun `instantiate and verify SongKey`() {
        val key = SongKey(urlPart = "p1", id = 42)
        assertEquals("p1", key.urlPart)
        assertEquals(42, key.id)
    }

    @Test
    fun `instantiate and verify Song with optional meta fields`() {
        val song = Song(
            id = 1,
            title = "Title",
            artist = "Artist",
            year = 2024,
            nasLocation = "sub/1.mp3",
            edition = "Ed",
            kuerzel = "ED",
            urlpart = "p1",
            image = "ed.jpg"
        )
        assertEquals(1, song.id)
        assertEquals("Title", song.title)
        assertEquals("Artist", song.artist)
        assertEquals(2024, song.year)
        assertEquals("sub/1.mp3", song.nasLocation)
        assertEquals("Ed", song.edition)
        assertEquals("ED", song.kuerzel)
        assertEquals("p1", song.urlpart)
        assertEquals("ed.jpg", song.image)
    }

    @Test
    fun `instantiate and verify MappingFile and MetaData`() {
        val meta = MetaData(edition = "Ed", kuerzel = "ED", urlpart = "p1", image = "cover.png")
        val file = MappingFile(meta = meta, titles = listOf(Song(1, "T", "A", 2020, "loc")))
        assertEquals("Ed", file.meta.edition)
        assertEquals("ED", file.meta.kuerzel)
        assertEquals(1, file.titles.size)
        assertNotNull(file.toString())
    }
}
