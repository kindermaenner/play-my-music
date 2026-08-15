package de.kindermaenner.playmymusic.data

import de.kindermaenner.playmymusic.domain.model.Track
import de.kindermaenner.playmymusic.offline.OfflineLibraryStorage
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class AppLocalTrackResolverTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var storage: OfflineLibraryStorage
    private lateinit var resolver: AppLocalTrackResolver
    private lateinit var editionDir: File

    @Before
    fun setup() {
        storage = mockk()
        editionDir = tempFolder.newFolder("EditionName")
        every { storage.editionDir("EditionName") } returns editionDir
        
        resolver = AppLocalTrackResolver(
            storage = storage,
            trackIdPadding = 4,
            trackFolderName = "tracks"
        )
    }

    @Test
    fun `resolves standard local path if file exists`() {
        val tracksDir = File(editionDir, "tracks")
        tracksDir.mkdirs()
        val trackFile = File(tracksDir, "0042.mp3")
        trackFile.writeText("content")

        val track = createTrack(id = 42, edition = "EditionName")
        
        val result = resolver.resolveLocalUri(track)
        
        assertEquals(trackFile.toURI().toString(), result)
    }

    @Test
    fun `returns null if standard local path file does not exist`() {
        val track = createTrack(id = 42, edition = "EditionName")
        
        val result = resolver.resolveLocalUri(track)
        
        assertNull(result)
    }

    @Test
    fun `resolves explicit nasLocation if file exists`() {
        val explicitFile = File(editionDir, "special/path.mp3")
        explicitFile.parentFile.mkdirs()
        explicitFile.writeText("content")

        val track = createTrack(edition = "EditionName", nasLocation = "special\\path.mp3")
        
        val result = resolver.resolveLocalUri(track)
        
        assertEquals(explicitFile.toURI().toString(), result)
    }

    @Test
    fun `falls back to track folder for explicit nasLocation if not at original path`() {
        val tracksDir = File(editionDir, "tracks")
        tracksDir.mkdirs()
        val trackFile = File(tracksDir, "path.mp3")
        trackFile.writeText("content")

        val track = createTrack(edition = "EditionName", nasLocation = "special\\path.mp3")
        
        val result = resolver.resolveLocalUri(track)
        
        assertEquals(trackFile.toURI().toString(), result)
    }

    private fun createTrack(id: Int = 1, edition: String = "", nasLocation: String = "") = Track(
        id = id,
        title = "T",
        artist = "A",
        year = 2024,
        edition = edition,
        editionAbbreviation = "E",
        urlPart = "u",
        image = "i",
        nasLocation = nasLocation
    )
}
