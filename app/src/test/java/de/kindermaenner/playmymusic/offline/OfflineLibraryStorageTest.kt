package de.kindermaenner.playmymusic.offline

import android.content.Context
import de.kindermaenner.playmymusic.domain.model.Track
import de.kindermaenner.playmymusic.settings.AppSettings
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
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
        assertEquals("Special _ Edition_", dir.name)
    }

    @Test
    fun `editionDir strips trailing slashes from directory names`() {
        val dirFromSmb = storage.editionDir("MyEdition/")
        val dirFromBackslash = storage.editionDir("MyEdition\\")
        val dirNormal = storage.editionDir("MyEdition")
        assertEquals("MyEdition", dirFromSmb.name)
        assertEquals("MyEdition", dirFromBackslash.name)
        assertEquals(dirNormal.name, dirFromSmb.name)
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
        explicitFile.parentFile?.mkdirs()
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

    @Test
    fun `resolveArtworkSource returns null when track image is blank`() {
        val track = Track(id = 1, title = "T", artist = "A", year = 2024, nasLocation = "", edition = "E", editionAbbreviation = "E", urlPart = "u", image = "   ")
        assertNull(storage.resolveArtworkSource(track))
    }

    @Test
    fun `resolveLocalTrackFile returns null when edition is blank`() {
        val track = Track(id = 1, title = "T", artist = "A", year = 2024, nasLocation = "", edition = "  ", editionAbbreviation = "E", urlPart = "u", image = "")
        assertNull(storage.resolveLocalTrackFile(track, AppSettings()))
    }

    @Test
    fun `resolveLocalTrackFile finds fallbackByName when candidate does not exist`() {
        val edition = "MyEdition"
        val editionDir = File(File(rootDir, "offline-library/editions"), edition).apply { mkdirs() }
        val tracksDir = File(editionDir, "tracks").apply { mkdirs() }
        val fallbackFile = File(tracksDir, "song.mp3").apply { writeText("audio") }

        val track = Track(id = 1, title = "T", artist = "A", year = 2024, nasLocation = "wrong/folder/song.mp3", edition = edition, editionAbbreviation = "ME", urlPart = "u", image = "")
        val resolved = storage.resolveLocalTrackFile(track, AppSettings(trackFolderName = "tracks"))

        assertEquals(fallbackFile.absolutePath, resolved?.absolutePath)
    }

    @Test
    fun `localMappingFiles discovers json files across edition directories`() {
        val ed1 = File(storage.editionsDir(), "Ed1").apply { mkdirs() }
        val ed2 = File(storage.editionsDir(), "Ed2").apply { mkdirs() }
        File(ed1, "mapping.json").apply { writeText("{}") }
        File(ed2, "other.json").apply { writeText("{}") }
        File(ed1, "ignore.txt").apply { writeText("txt") }

        val files = storage.localMappingFiles()
        assertEquals(2, files.size)
        assertTrue(files.any { it.name == "mapping.json" })
        assertTrue(files.any { it.name == "other.json" })
    }

    @Test
    fun `rootDir and editionsDir return correct file paths`() {
        assertEquals(File(rootDir, "offline-library"), storage.rootDir())
        assertEquals(File(rootDir, "offline-library/editions"), storage.editionsDir())
    }
}
