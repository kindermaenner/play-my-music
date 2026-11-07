package de.kindermaenner.playmymusic.offline

import android.content.Context
import de.kindermaenner.playmymusic.data.model.PlaybackSettings
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SmbAuthSettings
import io.mockk.every
import io.mockk.mockk
import jcifs.smb.SmbFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

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
}
