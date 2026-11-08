package de.kindermaenner.playmymusic.offline

import android.content.Context
import de.kindermaenner.playmymusic.data.model.PlaybackSettings
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SmbAuthSettings
import io.mockk.every
import io.mockk.mockk
import jcifs.smb.SmbFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.File
import java.io.IOException

class OfflineLibrarySyncServiceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private val context = mockk<Context>(relaxed = true)
    private lateinit var storage: OfflineLibraryStorage
    private lateinit var service: OfflineLibrarySyncService

    @Before
    fun setUp() {
        val filesDir = tempFolder.newFolder("files")
        every { context.filesDir } returns filesDir
        storage = OfflineLibraryStorage(context)
        service = OfflineLibrarySyncService(context, storage = storage)
    }

    @Test
    fun `buildBaseUri creates correct smb url`() {
        val settings = AppSettings(
            playback = PlaybackSettings(nasAddress = "192.168.1.1", nasBaseFolder = "music\\tracks")
        )
        val uri = service.buildBaseUri(settings)
        assertEquals("smb://192.168.1.1/music/tracks/", uri)
    }

    @Test
    fun `buildBaseUri handles empty base folder`() {
        val settings = AppSettings(
            playback = PlaybackSettings(nasAddress = "my-nas", nasBaseFolder = "")
        )
        val uri = service.buildBaseUri(settings)
        assertEquals("smb://my-nas/", uri)
    }

    @Test
    fun `buildCandidateContexts includes auth when username provided`() {
        val auth = SmbAuthSettings(username = "user", password = "pwd", domain = "work")
        val contexts = service.buildCandidateContexts(auth)
        // 3 anonymous + 1 configured user + 1 base
        assertEquals(5, contexts.size)
    }

    @Test
    fun `buildCandidateContexts only uses anonymous when username empty`() {
        val auth = SmbAuthSettings(username = "", password = "", domain = "")
        val contexts = service.buildCandidateContexts(auth)
        // 3 anonymous + 1 base
        assertEquals(4, contexts.size)
    }

    @Test
    fun `shouldSkipCopy returns false when target does not exist`() {
        val source = mockk<SmbFile>()
        every { source.length() } returns 100L
        every { source.lastModified() } returns 1000L

        val target = File(tempFolder.root, "not_existing.mp3")

        assertFalse(service.shouldSkipCopy(source, target))
    }

    @Test
    fun `shouldSkipCopy returns false when sizes differ`() {
        val target = tempFolder.newFile("song.mp3").apply {
            writeText("short")
        }
        val source = mockk<SmbFile>()
        every { source.length() } returns 999L
        every { source.lastModified() } returns target.lastModified()

        assertFalse(service.shouldSkipCopy(source, target))
    }

    @Test
    fun `shouldSkipCopy returns false when timestamp differs by more than tolerance`() {
        val target = tempFolder.newFile("song2.mp3").apply {
            writeText("same length")
        }
        val source = mockk<SmbFile>()
        every { source.length() } returns target.length()
        // 5 seconds difference > 2000ms tolerance
        every { source.lastModified() } returns target.lastModified() + 5000L

        assertFalse(service.shouldSkipCopy(source, target))
    }

    @Test
    fun `shouldSkipCopy returns true when size and timestamp match within tolerance`() {
        val target = tempFolder.newFile("song3.mp3").apply {
            writeText("exact match")
        }
        val source = mockk<SmbFile>()
        every { source.length() } returns target.length()
        // 500ms difference <= 2000ms tolerance
        every { source.lastModified() } returns target.lastModified() + 500L

        assertTrue(service.shouldSkipCopy(source, target))
    }

    @Test
    fun `copySmbFileAtomic copies content to target and sets timestamp`() {
        val content = "music data stream"
        val source = mockk<SmbFile>()
        every { source.inputStream } answers { ByteArrayInputStream(content.toByteArray()) }
        every { source.name } returns "remote_song.mp3"
        every { source.length() } returns content.length.toLong()
        every { source.lastModified() } returns 123456000L

        val target = File(tempFolder.root, "sub/downloaded.mp3")

        val copied = service.copySmbFileAtomic(source, target)

        assertTrue(copied)
        assertTrue(target.exists())
        assertEquals(content, target.readText())
        assertFalse(File(target.parentFile, "${target.name}.tmp").exists())
    }

    @Test
    fun `copySmbFileAtomic skips copying if file is already up to date`() {
        val target = tempFolder.newFile("existing.mp3").apply {
            writeText("same content")
        }
        val source = mockk<SmbFile>()
        every { source.length() } returns target.length()
        every { source.lastModified() } returns target.lastModified()

        val copied = service.copySmbFileAtomic(source, target)

        assertFalse(copied)
    }

    @Test
    fun `syncMetadata throws IOException when NAS base directory cannot be accessed`() = runBlocking {
        val settings = AppSettings(
            playback = PlaybackSettings(nasAddress = "non_existent_host_12345", nasBaseFolder = "music")
        )

        try {
            service.syncMetadata(settings)
            fail("Expected IOException when NAS is unreachable")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("Unable to access NAS base directory") == true)
        }
    }

    @Test
    fun `syncTracks throws IOException when NAS base directory cannot be accessed`() = runBlocking {
        val settings = AppSettings(
            playback = PlaybackSettings(nasAddress = "non_existent_host_12345", nasBaseFolder = "music")
        )

        try {
            service.syncTracks(settings)
            fail("Expected IOException when NAS is unreachable")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("Unable to access NAS base directory") == true)
        }
    }

    @Test
    fun `buildBaseUri formats URI correctly with and without base folder`() {
        val s1 = AppSettings(playback = PlaybackSettings(nasAddress = "\\\\192.168.1.50\\", nasBaseFolder = "/music/audio/"))
        assertEquals("smb://192.168.1.50/music/audio/", service.buildBaseUri(s1))

        val s2 = AppSettings(playback = PlaybackSettings(nasAddress = "nas.local", nasBaseFolder = ""))
        assertEquals("smb://nas.local/", service.buildBaseUri(s2))

        val s3 = AppSettings(playback = PlaybackSettings(nasAddress = "nas.local", nasBaseFolder = "   "))
        assertEquals("smb://nas.local/", service.buildBaseUri(s3))
    }

    @Test
    fun `buildCandidateContexts builds contexts for anonymous and authenticated user`() {
        val noAuth = SmbAuthSettings(username = "", password = "", domain = "")
        val contextsNoAuth = service.buildCandidateContexts(noAuth)
        assertEquals(4, contextsNoAuth.size)

        val userWithoutDomain = SmbAuthSettings(username = "myuser", password = "secretPassword", domain = "")
        val contextsUser = service.buildCandidateContexts(userWithoutDomain)
        assertEquals(5, contextsUser.size)

        val userWithDomain = SmbAuthSettings(username = "myuser", password = "secretPassword", domain = "WORKGROUP")
        val contextsDomain = service.buildCandidateContexts(userWithDomain)
        assertEquals(5, contextsDomain.size)
    }

    @Test
    fun `syncMetadata copies mapping and image but no tracks`() = runBlocking {
        val rootMock = mockk<SmbFile>()
        every { rootMock.exists() } returns true
        every { rootMock.isDirectory } returns true

        val editionDirMock = mockk<SmbFile>()
        every { editionDirMock.name } returns "EditionA"
        every { editionDirMock.isDirectory } returns true
        every { rootMock.listFiles() } returns arrayOf(editionDirMock)

        val mappingJsonContent = """
            {
               "meta": { "edition": "EditionA", "kuerzel": "EA", "urlpart": "p1", "image": "cover.jpg" },
               "titles": []
            }
        """.trimIndent()
        val mappingSmbMock = mockk<SmbFile>()
        every { mappingSmbMock.isFile } returns true
        every { mappingSmbMock.isDirectory } returns false
        every { mappingSmbMock.name } returns "edition_a.json"
        every { mappingSmbMock.inputStream } answers { ByteArrayInputStream(mappingJsonContent.toByteArray()) }
        every { mappingSmbMock.length() } returns mappingJsonContent.length.toLong()
        every { mappingSmbMock.lastModified() } returns 1000L

        every { editionDirMock.listFiles() } returns arrayOf(mappingSmbMock)

        val imageSmbMock = mockk<SmbFile>()
        every { imageSmbMock.exists() } returns true
        every { imageSmbMock.isFile } returns true
        every { imageSmbMock.isDirectory } returns false
        val imageBytes = byteArrayOf(1, 2, 3)
        every { imageSmbMock.inputStream } answers { ByteArrayInputStream(imageBytes) }
        every { imageSmbMock.length() } returns imageBytes.size.toLong()
        every { imageSmbMock.lastModified() } returns 2000L

        val testService = OfflineLibrarySyncService(
            context = context,
            storage = storage,
            smbFileOpener = { _, _ -> rootMock },
            smbChildOpener = { _, child ->
                if (child == "cover.jpg") imageSmbMock else mockk(relaxed = true)
            }
        )

        val result = testService.syncMetadata(AppSettings())
        assertEquals(1, result.copiedMappings)
        assertEquals(1, result.copiedImages)
        assertEquals(0, result.copiedTracks)
        assertEquals(0, result.totalTracks)

        val localEdDir = storage.editionDir("EditionA")
        assertTrue(File(localEdDir, "edition_a.json").exists())
        assertTrue(File(localEdDir, "cover.jpg").exists())
    }

    @Test
    fun `syncTracks copies tracks and notifies progress`() = runBlocking {
        val rootMock = mockk<SmbFile>()
        every { rootMock.exists() } returns true
        every { rootMock.isDirectory } returns true

        val editionDirMock = mockk<SmbFile>()
        every { editionDirMock.name } returns "EditionA"
        every { editionDirMock.isDirectory } returns true
        every { rootMock.listFiles() } returns arrayOf(editionDirMock)

        val mappingJsonContent = """
            {
               "meta": { "edition": "EditionA", "kuerzel": "EA", "urlpart": "p1", "image": "" },
               "titles": []
            }
        """.trimIndent()
        val mappingSmbMock = mockk<SmbFile>()
        every { mappingSmbMock.isFile } returns true
        every { mappingSmbMock.isDirectory } returns false
        every { mappingSmbMock.name } returns "edition_a.json"
        every { mappingSmbMock.inputStream } answers { ByteArrayInputStream(mappingJsonContent.toByteArray()) }
        every { mappingSmbMock.length() } returns mappingJsonContent.length.toLong()
        every { mappingSmbMock.lastModified() } returns 1000L

        every { editionDirMock.listFiles() } returns arrayOf(mappingSmbMock)

        val tracksFolderMock = mockk<SmbFile>()
        every { tracksFolderMock.name } returns "tracks"
        every { tracksFolderMock.exists() } returns true
        every { tracksFolderMock.isDirectory } returns true

        val track1 = mockk<SmbFile>()
        every { track1.name } returns "0001.mp3"
        every { track1.isDirectory } returns false
        every { track1.isFile } returns true
        every { track1.inputStream } answers { ByteArrayInputStream("audio1".toByteArray()) }
        every { track1.length() } returns 6L
        every { track1.lastModified() } returns 3000L

        every { tracksFolderMock.listFiles() } returns arrayOf(track1)

        val testService = OfflineLibrarySyncService(
            context = context,
            storage = storage,
            smbFileOpener = { _, _ -> rootMock },
            smbChildOpener = { _, child ->
                if (child == "tracks/") tracksFolderMock else mockk(relaxed = true)
            }
        )

        val progressList = mutableListOf<SyncProgress>()
        val result = testService.syncTracks(AppSettings()) { progress ->
            progressList.add(progress)
        }

        assertEquals(1, result.copiedMappings)
        assertEquals(0, result.copiedImages)
        assertEquals(1, result.copiedTracks)
        assertEquals(1, result.totalTracks)
        assertEquals(1, progressList.size)
        assertEquals("0001.mp3", progressList[0].currentItem)
        assertEquals(1, progressList[0].current)
        assertEquals(1, progressList[0].total)
    }

    @Test
    fun `sync handles edition directory without mapping file gracefully`() = runBlocking {
        val rootMock = mockk<SmbFile>()
        every { rootMock.exists() } returns true
        every { rootMock.isDirectory } returns true

        val editionDirMock = mockk<SmbFile>()
        every { editionDirMock.name } returns "NoMappingEdition"
        every { editionDirMock.isDirectory } returns true
        every { rootMock.listFiles() } returns arrayOf(editionDirMock)

        // No .json file
        val txtFile = mockk<SmbFile>()
        every { txtFile.isFile } returns true
        every { txtFile.isDirectory } returns false
        every { txtFile.name } returns "readme.txt"
        every { editionDirMock.listFiles() } returns arrayOf(txtFile)

        val testService = OfflineLibrarySyncService(
            context = context,
            storage = storage,
            smbFileOpener = { _, _ -> rootMock }
        )

        val result = testService.syncMetadata(AppSettings())
        assertEquals(0, result.copiedMappings)
        assertEquals(0, result.copiedImages)
        assertEquals(0, result.copiedTracks)
    }

    @Test
    fun `sync relocates local files when meta edition name differs from directory name`() = runBlocking {
        val rootMock = mockk<SmbFile>()
        every { rootMock.exists() } returns true
        every { rootMock.isDirectory } returns true

        val editionDirMock = mockk<SmbFile>()
        every { editionDirMock.name } returns "FolderDiffers"
        every { editionDirMock.isDirectory } returns true
        every { rootMock.listFiles() } returns arrayOf(editionDirMock)

        val mappingJsonContent = """
            {
               "meta": { "edition": "RealEditionName", "kuerzel": "RE", "urlpart": "p1", "image": "" },
               "titles": []
            }
        """.trimIndent()
        val mappingSmbMock = mockk<SmbFile>()
        every { mappingSmbMock.isFile } returns true
        every { mappingSmbMock.isDirectory } returns false
        every { mappingSmbMock.name } returns "map.json"
        every { mappingSmbMock.inputStream } answers { ByteArrayInputStream(mappingJsonContent.toByteArray()) }
        every { mappingSmbMock.length() } returns mappingJsonContent.length.toLong()
        every { mappingSmbMock.lastModified() } returns 1000L

        every { editionDirMock.listFiles() } returns arrayOf(mappingSmbMock)

        val testService = OfflineLibrarySyncService(
            context = context,
            storage = storage,
            smbFileOpener = { _, _ -> rootMock }
        )

        val result = testService.syncMetadata(AppSettings())
        assertEquals(1, result.copiedMappings)

        val realMetaDir = storage.editionDir("RealEditionName")
        assertTrue(File(realMetaDir, "map.json").exists())
    }

    @Test
    fun `copyDirectoryRecursive copies subdirectories recursively`() = runBlocking {
        val rootMock = mockk<SmbFile>()
        every { rootMock.exists() } returns true
        every { rootMock.isDirectory } returns true

        val editionDirMock = mockk<SmbFile>()
        every { editionDirMock.name } returns "EditionSub"
        every { editionDirMock.isDirectory } returns true
        every { rootMock.listFiles() } returns arrayOf(editionDirMock)

        val mappingJsonContent = """
            {
               "meta": { "edition": "EditionSub", "kuerzel": "ES", "urlpart": "p1", "image": "" },
               "titles": []
            }
        """.trimIndent()
        val mappingSmbMock = mockk<SmbFile>()
        every { mappingSmbMock.isFile } returns true
        every { mappingSmbMock.isDirectory } returns false
        every { mappingSmbMock.name } returns "edition_sub.json"
        every { mappingSmbMock.inputStream } answers { ByteArrayInputStream(mappingJsonContent.toByteArray()) }
        every { mappingSmbMock.length() } returns mappingJsonContent.length.toLong()
        every { mappingSmbMock.lastModified() } returns 1000L

        every { editionDirMock.listFiles() } returns arrayOf(mappingSmbMock)

        val tracksFolderMock = mockk<SmbFile>()
        every { tracksFolderMock.name } returns "tracks"
        every { tracksFolderMock.exists() } returns true
        every { tracksFolderMock.isDirectory } returns true

        val subDirMock = mockk<SmbFile>()
        every { subDirMock.name } returns "subfolder"
        every { subDirMock.isDirectory } returns true
        every { subDirMock.isFile } returns false

        val trackInSub = mockk<SmbFile>()
        every { trackInSub.name } returns "nested.mp3"
        every { trackInSub.isDirectory } returns false
        every { trackInSub.isFile } returns true
        every { trackInSub.inputStream } answers { ByteArrayInputStream("nested audio".toByteArray()) }
        every { trackInSub.length() } returns 12L
        every { trackInSub.lastModified() } returns 4000L

        every { tracksFolderMock.listFiles() } returns arrayOf(subDirMock)
        every { subDirMock.listFiles() } returns arrayOf(trackInSub)

        val testService = OfflineLibrarySyncService(
            context = context,
            storage = storage,
            smbFileOpener = { _, _ -> rootMock },
            smbChildOpener = { _, child ->
                if (child == "tracks/") tracksFolderMock else mockk(relaxed = true)
            }
        )

        val result = testService.syncTracks(AppSettings())
        assertEquals(1, result.copiedTracks)
        val targetNestedFile = File(storage.editionDir("EditionSub"), "tracks/subfolder/nested.mp3")
        assertTrue(targetNestedFile.exists())
        assertEquals("nested audio", targetNestedFile.readText())
    }
}
