package de.kindermaenner.playmymusic.core.mapping

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import de.kindermaenner.playmymusic.core.model.SongKey
import org.junit.Test

class MappingLoaderTest {

    @Test
    fun `parse builds correct song map`() {
        val json = """
        {
          "meta": {
            "edition": "Testdaten-Edition",
            "kuerzel": "TDE",
            "urlpart": "nonexistent",
            "image": "testdaten-edition.jpg"
          },
          "titles": [
            {
              "artist": "Coole Band",
              "title": "ABC-Lied",
              "year": 1234,
              "nasLocation": "",
              "id": 1
            },
            {
              "artist": "Noch coolere Band",
              "title": "123-Lied",
              "year": 2345,
              "nasLocation": "ordner1",
              "id": 2
            },
            {
              "artist": "Uncoole Band",
              "title": "The best song of the world",
              "year": 4567,
              "nasLocation": "ordner2",
              "id": 3
            }
          ]
        }
        """.trimIndent()

        val result = MappingLoader.parse(listOf(json))

        assertEquals(3, result.size)

        val song1 = result[SongKey(urlPart = "nonexistent", id = 1)]!!
        assertEquals("Testdaten-Edition", song1.edition)
        assertEquals("TDE", song1.kuerzel)
        assertEquals("nonexistent", song1.urlpart)
        assertEquals("testdaten-edition.jpg", song1.image)
        assertEquals("Coole Band", song1.artist)
        assertEquals("ABC-Lied", song1.title)
        assertEquals(1234, song1.year)
        assertEquals("", song1.nasLocation)

        val song2 = result[SongKey(urlPart = "nonexistent", id = 2)]!!
        assertEquals("Testdaten-Edition", song2.edition)
        assertEquals("TDE", song2.kuerzel)
        assertEquals("nonexistent", song2.urlpart)
        assertEquals("testdaten-edition.jpg", song2.image)
        assertEquals("Noch coolere Band", song2.artist)
        assertEquals("123-Lied", song2.title)
        assertEquals(2345, song2.year)
        assertEquals("ordner1", song2.nasLocation)

        val song3 = result[SongKey(urlPart = "nonexistent", id = 3)]!!
        assertEquals("Testdaten-Edition", song3.edition)
        assertEquals("TDE", song3.kuerzel)
        assertEquals("nonexistent", song3.urlpart)
        assertEquals("testdaten-edition.jpg", song3.image)
        assertEquals("Uncoole Band", song3.artist)
        assertEquals("The best song of the world", song3.title)
        assertEquals(4567, song3.year)
        assertEquals("ordner2", song3.nasLocation)
    }

    @Test
    fun `parse caches results`() {
        val json = """
        {
          "meta": {
            "edition": "Testdaten-Edition",
            "kuerzel": "TDE",
            "urlpart": "nonexistent",
            "image": "testdaten-edition.jpg"
          },
          "titles": [
            {
              "artist": "Coole Band",
              "title": "ABC-Lied",
              "year": 1234,
              "nasLocation": "",
              "id": 1
            },
            {
              "artist": "Noch coolere Band",
              "title": "123-Lied",
              "year": 2345,
              "nasLocation": "ordner1",
              "id": 2
            },
            {
              "artist": "Uncoole Band",
              "title": "The best song of the world",
              "year": 4567,
              "nasLocation": "ordner2",
              "id": 3
            }
          ]
        }
        """.trimIndent()

        val first = MappingLoader.parse(listOf(json))
        val second = MappingLoader.parse(listOf(json))

        assertSame(first, second)
    }
}
