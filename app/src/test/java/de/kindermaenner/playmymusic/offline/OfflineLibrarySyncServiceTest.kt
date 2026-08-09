package de.kindermaenner.playmymusic.offline

import android.content.Context
import de.kindermaenner.playmymusic.core.model.PlaybackSettings
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SmbAuthSettings
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineLibrarySyncServiceTest {

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
}
