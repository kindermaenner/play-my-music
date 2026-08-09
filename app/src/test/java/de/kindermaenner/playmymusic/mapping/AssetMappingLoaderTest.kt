package de.kindermaenner.playmymusic.mapping

import android.content.Context
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AssetMappingLoaderTest {

    private val context = mockk<Context>()
    private val loader = AssetMappingLoader(context)

    @Test
    fun `matchesFileFilter returns true for empty filter`() {
        assertTrue(loader.matchesFileFilter("any.json", ""))
        assertTrue(loader.matchesFileFilter("any.json", "   "))
    }

    @Test
    fun `matchesFileFilter matches by suffix`() {
        assertTrue(loader.matchesFileFilter("my_mapping.json", "mapping"))
        assertTrue(loader.matchesFileFilter("game_de.json", "de"))
    }

    @Test
    fun `matchesFileFilter matches by prefix`() {
        assertTrue(loader.matchesFileFilter("edition1_songs.json", "edition1"))
    }

    @Test
    fun `matchesFileFilter returns false for non-matching strings`() {
        assertFalse(loader.matchesFileFilter("rock.json", "pop"))
        assertFalse(loader.matchesFileFilter("mapping_1.json", "mapping_2"))
    }
}
