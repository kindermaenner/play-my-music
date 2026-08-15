package de.kindermaenner.playmymusic.offline

import android.content.Context
import de.kindermaenner.playmymusic.domain.model.Track
import de.kindermaenner.playmymusic.settings.AppSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class OfflineLibraryStorageTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var storage: OfflineLibraryStorage
    private lateinit var rootDir: File

    @Before
    fun setup() {
        context = mockk()
        rootDir = tempFolder.newFolder("files")
        every { context.filesDir } returns rootDir
        storage = OfflineLibraryStorage(context)
    }

    @Test
    fun `editionDir sanitizes edition name`() {
        val dir = storage.editionDir("Special / Edition!")
        // Sanitizer: input.trim().replace(Regex("[^A-Za-z0-9._ -]"), "_")
        // "Special / Edition!" -> "Special _ Edition_"
        assertEquals("Special _ Edition_", dir.name)
    }

    @Test
    fun `resolveLocalTrackFile returns file if it exists`() {
        val edition = "MyEdition"
        val editionDir = File(File(rootDir, "offline-library/editions"), edition)
        editionDir.mkdirs()
        
        val tracksDir = File(editionDir, "tracks")
        tracksDir.mkdirs()
        
        val trackFile = File(tracksDir, "0042.mp3")
        trackFile.writeText("dummy content")

        val track = Track(id = 42, title = "T", artist = "A", year = 2024, nasLocation = "", edition = edition, editionAbbreviation = "ME", urlPart = "url", image = "")
        val appSettings = AppSettings(trackIdPadding = 4, trackFolderName = "tracks")

        val resolved = storage.resolveLocalTrackFile(track, appSettings)
        
        assertEquals(trackFile.absolutePath, resolved?.absolutePath)
    }

    @Test
    fun `resolveLocalTrackFile handles explicit nasLocation`() {
        val edition = "MyEdition"
        val editionDir = File(File(rootDir, "offline-library/editions"), edition)
        editionDir.mkdirs()
        
        val explicitFile = File(editionDir, "special/path/song.mp3")
        explicitFile.parentFile.mkdirs()
        explicitFile.writeText("dummy content")

        val track = Track(id = 1, title = "T", artist = "A", year = 2024, nasLocation = "special\\path\\song.mp3", edition = edition, editionAbbreviation = "ME", urlPart = "url", image = "")
        val appSettings = AppSettings()

        val resolved = storage.resolveLocalTrackFile(track, appSettings)
        
        assertEquals(explicitFile.absolutePath, resolved?.absolutePath)
    }

    @Test
    fun `resolveArtworkSource prefers local file over asset`() {
        val edition = "MyEdition"
        val editionDir = File(File(rootDir, "offline-library/editions"), edition)
        editionDir.mkdirs()
        
        val imageFile = File(editionDir, "cover.jpg")
        imageFile.writeText("image data")

        val track = Track(id = 1, title = "T", artist = "A", year = 2024, nasLocation = "", edition = edition, editionAbbreviation = "ME", urlPart = "url", image = "cover.jpg")
        
        val result = storage.resolveArtworkSource(track)
        
        assertTrue(result is File)
        assertEquals(imageFile.absolutePath, (result as File).absolutePath)
    }

    @Test
    fun `resolveArtworkSource falls back to asset if local file missing`() {
        val track = Track(id = 1, title = "T", artist = "A", year = 2024, nasLocation = "", edition = "Missing", editionAbbreviation = "M", urlPart = "u", image = "cover.jpg")
        
        val result = storage.resolveArtworkSource(track)
        
        assertEquals("file:///android_asset/cover.jpg", result)
    }
}
