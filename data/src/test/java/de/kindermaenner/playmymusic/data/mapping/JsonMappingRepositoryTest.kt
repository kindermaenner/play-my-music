package de.kindermaenner.playmymusic.data.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class JsonMappingRepositoryTest {

    @Test
    fun `load correctly maps all fields from JSON to Domain model`() {
        val json = """
        {
          "meta": {
            "edition": "Test Edition",
            "kuerzel": "TE",
            "urlpart": "p1",
            "image": "cover.jpg"
          },
          "titles": [
            {
              "id": 42,
              "artist": "The Artist",
              "title": "The Title",
              "year": 2024,
              "nasLocation": "custom/path.mp3"
            }
          ]
        }
        """.trimIndent()

        val repository = JsonMappingRepository()
        repository.load(listOf(json))

        val track = repository.getTrack("p1", 42)
        assertNotNull(track)
        assertEquals(42, track?.id)
        assertEquals("The Title", track?.title)
        assertEquals("The Artist", track?.artist)
        assertEquals(2024, track?.year)
        assertEquals("Test Edition", track?.edition)
        assertEquals("TE", track?.editionAbbreviation)
        assertEquals("p1", track?.urlPart)
        assertEquals("cover.jpg", track?.image)
        assertEquals("custom/path.mp3", track?.nasLocation)
    }

    @Test
    fun `load merges multiple files`() {
        val json1 = """
        {
          "meta": { "edition": "E1", "kuerzel": "K1", "urlpart": "p1", "image": "i1" },
          "titles": [ { "artist": "A1", "title": "T1", "year": 1, "nasLocation": "", "id": 1 } ]
        }
        """.trimIndent()
        val json2 = """
        {
          "meta": { "edition": "E2", "kuerzel": "K2", "urlpart": "p2", "image": "i2" },
          "titles": [ { "artist": "A2", "title": "T2", "year": 2, "nasLocation": "", "id": 2 } ]
        }
        """.trimIndent()

        val repository = JsonMappingRepository()
        repository.load(listOf(json1, json2))

        assertNotNull(repository.getTrack("p1", 1))
        assertNotNull(repository.getTrack("p2", 2))
    }
}
