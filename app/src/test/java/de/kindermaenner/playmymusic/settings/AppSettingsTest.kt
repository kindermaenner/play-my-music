package de.kindermaenner.playmymusic.settings

import de.kindermaenner.playmymusic.core.model.PlaybackSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class AppSettingsTest {

    @Test
    fun `merge prefers override values over base values`() {
        val base = AppSettings(
            playback = PlaybackSettings("BASE_NAS", "BASE_FOLDER"),
            qrValidation = "BASE_QR",
            trackIdPadding = 4,
            trackFolderName = "BASE_TRACKS"
        )
        val override = AppSettings(
            playback = PlaybackSettings("OVERRIDE_NAS", "OVERRIDE_FOLDER"),
            qrValidation = "OVERRIDE_QR",
            trackIdPadding = 2,
            trackFolderName = "OVERRIDE_TRACKS"
        )

        val result = Settings.merge(base, override)

        assertEquals("OVERRIDE_NAS", result.playback.nasAddress)
        assertEquals("OVERRIDE_FOLDER", result.playback.nasBaseFolder)
        assertEquals("OVERRIDE_QR", result.qrValidation)
        assertEquals(2, result.trackIdPadding)
        assertEquals("OVERRIDE_TRACKS", result.trackFolderName)
    }

    @Test
    fun `merge keeps base values when override values are blank`() {
        val base = AppSettings(
            playback = PlaybackSettings("BASE_NAS", "BASE_FOLDER"),
            qrValidation = "BASE_QR",
            trackIdPadding = 4,
            trackFolderName = "BASE_TRACKS"
        )
        val override = Settings.emptyOverrideSettings()

        val result = Settings.merge(base, override)

        assertEquals("BASE_NAS", result.playback.nasAddress)
        assertEquals("BASE_FOLDER", result.playback.nasBaseFolder)
        assertEquals("BASE_QR", result.qrValidation)
        assertEquals(4, result.trackIdPadding)
        assertEquals("BASE_TRACKS", result.trackFolderName)
    }

    @Test
    fun `toAppSettings correctly maps SettingsFile`() {
        val file = SettingsFile(
            nasAddress = "NAS",
            nasBaseFolder = "FOLDER",
            qrValidation = "QR",
            trackIdPadding = 3,
            smbUsername = "user"
        )

        val settings = file.toAppSettings()

        assertEquals("NAS", settings.playback.nasAddress)
        assertEquals("FOLDER", settings.playback.nasBaseFolder)
        assertEquals("QR", settings.qrValidation)
        assertEquals(3, settings.trackIdPadding)
        assertEquals("user", settings.smbAuth.username)
    }
}
