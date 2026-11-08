package de.kindermaenner.playmymusic.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.datastore.preferences.core.edit
import de.kindermaenner.playmymusic.data.model.PlaybackSettings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository

    @Before
    fun setUp() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        context.settingsDataStore.edit { it.clear() }
        repository = SettingsRepository(context)
    }

    @Test
    fun `default values are emitted initially`() = runTest {
        val debugMode = repository.debugMode.first()
        assertFalse(debugMode)

        val settings = repository.appSettings.first()
        val expectedDefaults = Settings.defaultAppSettings(context)
        assertEquals(expectedDefaults, settings)
    }

    @Test
    fun `setDebugMode updates debugMode flow`() = runTest {
        repository.setDebugMode(true)
        assertTrue(repository.debugMode.first())

        repository.setDebugMode(false)
        assertFalse(repository.debugMode.first())
    }

    @Test
    fun `setAppSettings persists all fields and emits updated settings`() = runTest {
        val newSettings = AppSettings(
            playback = PlaybackSettings(
                nasAddress = "192.168.1.100",
                nasBaseFolder = "Audio/Music"
            ),
            qrValidation = "https://mygame.org/track/",
            mappingFilePostfix = "custom_map",
            trackIdPadding = 4,
            trackFolderName = "songs",
            smbAuth = SmbAuthSettings(
                username = "admin",
                password = "secret_password",
                domain = "MYDOMAIN"
            )
        )

        repository.setAppSettings(newSettings)

        val loaded = repository.appSettings.first()
        assertEquals("192.168.1.100", loaded.playback.nasAddress)
        assertEquals("Audio/Music", loaded.playback.nasBaseFolder)
        assertEquals("https://mygame.org/track/", loaded.qrValidation)
        assertEquals("custom_map", loaded.mappingFilePostfix)
        assertEquals(4, loaded.trackIdPadding)
        assertEquals("songs", loaded.trackFolderName)
        assertEquals("admin", loaded.smbAuth.username)
        assertEquals("secret_password", loaded.smbAuth.password)
        assertEquals("MYDOMAIN", loaded.smbAuth.domain)
    }
}
