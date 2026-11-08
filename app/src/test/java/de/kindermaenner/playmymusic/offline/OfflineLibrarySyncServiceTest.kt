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
    private val service = OfflineLibrarySyncService(context)

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
        
        // guest, anonymous, empty, configured, singleton
        assertTrue(contexts.size >= 4)
    }

    @Test
    fun `shouldSkipCopy returns false if target does not exist`() {
        val source = mockk<SmbFile>()
        val target = File(tempFolder.root, "non_existent.mp3")

        assertFalse(service.shouldSkipCopy(source, target))
    }

    @Test
    fun `shouldSkipCopy returns false if file length differs`() {
        val target = tempFolder.newFile("song.mp3").apply {
            writeText("short")
        }
        val source = mockk<SmbFile>()
        every { source.length() } returns target.length() + 100
        every { source.lastModified() } returns target.lastModified()

        assertFalse(service.shouldSkipCopy(source, target))
    }

    @Test
    fun `shouldSkipCopy returns false if remote is modified much later`() {
        val target = tempFolder.newFile("song.mp3").apply {
            writeText("audio content")
        }
        val source = mockk<SmbFile>()
        every { source.length() } returns target.length()
        every { source.lastModified() } returns target.lastModified() + 60_000 // 1 minute later

        assertFalse(service.shouldSkipCopy(source, target))
    }

    @Test
    fun `shouldSkipCopy returns true if length and lastModified match`() {
        val target = tempFolder.newFile("song.mp3").apply {
            writeText("audio content")
        }
        val source = mockk<SmbFile>()
        every { source.length() } returns target.length()
        every { source.lastModified() } returns target.lastModified()

        assertTrue(service.shouldSkipCopy(source, target))
    }

    @Test
    fun `copySmbFileAtomic copies content and sets lastModified`() {
        val content = "sample mp3 bytes"
        val remoteTime = 1700000000000L
        val source = mockk<SmbFile>()
        every { source.inputStream } answers { ByteArrayInputStream(content.toByteArray()) }
        every { source.length() } returns content.length.toLong()
        every { source.lastModified() } returns remoteTime

        val target = File(tempFolder.root, "sub/downloaded.mp3")

        val copied = service.copySmbFileAtomic(source, target)

        assertTrue(copied)
        assertTrue(target.exists())
        assertEquals(content, target.readText())
        // temp file should have been cleaned up/renamed
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
        // 3 anonymous + 1 base
        assertEquals(4, contextsNoAuth.size)

        val userWithoutDomain = SmbAuthSettings(username = "myuser", password = "secretPassword", domain = "")
        val contextsUser = service.buildCandidateContexts(userWithoutDomain)
        // 3 anonymous + 1 configured user + 1 base
        assertEquals(5, contextsUser.size)

        val userWithDomain = SmbAuthSettings(username = "myuser", password = "secretPassword", domain = "WORKGROUP")
        val contextsDomain = service.buildCandidateContexts(userWithDomain)
        assertEquals(5, contextsDomain.size)
    }
}
