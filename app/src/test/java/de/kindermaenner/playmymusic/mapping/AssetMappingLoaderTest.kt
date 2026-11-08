package de.kindermaenner.playmymusic.mapping

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AssetMappingLoaderTest {

    private lateinit var context: Context
    private lateinit var loader: AssetMappingLoader

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        loader = AssetMappingLoader(context)
    }

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

    @Test
    fun `loadJsonFiles includes matching offline mapping files`() {
        val offlineDir = File(context.filesDir, "offline-library/editions/EditionA").apply { mkdirs() }
        val matchingFile = File(offlineDir, "game_de.json").apply { writeText("{\"edition\":\"EditionA\"}") }
        val nonMatchingFile = File(offlineDir, "game_en.json").apply { writeText("{\"edition\":\"EditionEn\"}") }

        val contents = loader.loadJsonFiles("de")

        assertTrue(contents.contains("{\"edition\":\"EditionA\"}"))
        assertFalse(contents.contains("{\"edition\":\"EditionEn\"}"))

        matchingFile.delete()
        nonMatchingFile.delete()
    }
}
