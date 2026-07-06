package de.kindermaenner.playmymusic.settings

import android.content.Context
import com.google.gson.Gson
import de.kindermaenner.playmymusic.core.model.PlaybackSettings

object Settings {

    private const val DEFAULT_TRACK_ID_PADDING = 4
    private const val DEFAULT_SETTINGS_PATH = "settings/settings.json"
    private const val LOCAL_SETTINGS_PATH = "local/settings/settings.json"

    @Volatile
    private var cachedDefaults: AppSettings? = null

    fun defaultAppSettings(context: Context): AppSettings {
        cachedDefaults?.let { return it }

        synchronized(this) {
            cachedDefaults?.let { return it }

            val defaultSettings = readSettingsFile(
                context = context,
                assetPath = DEFAULT_SETTINGS_PATH,
                fallback = AppSettings(trackIdPadding = DEFAULT_TRACK_ID_PADDING)
            )
            val localSettings = readSettingsFile(
                context = context,
                assetPath = LOCAL_SETTINGS_PATH,
                fallback = emptyOverrideSettings()
            )

            return defaultSettings.merge(localSettings).also {
                cachedDefaults = it
            }
        }
    }

    private fun readSettingsFile(
        context: Context,
        assetPath: String,
        fallback: AppSettings
    ): AppSettings {
        val json = runCatching {
            context.assets.open(assetPath).bufferedReader().use { it.readText() }
        }.getOrElse {
            return fallback
        }

        return runCatching {
            Gson().fromJson(json, SettingsFile::class.java).toAppSettings()
        }.getOrElse {
            fallback
        }
    }

    private fun emptyOverrideSettings(): AppSettings {
        return AppSettings(
            playback = PlaybackSettings(nasAddress = "", nasBaseFolder = ""),
            qrValidation = "",
            mappingFilePostfix = "",
            trackIdPadding = 0,
            trackFolderName = "",
            smbAuth = SmbAuthSettings()
        )
    }

    private fun AppSettings.merge(override: AppSettings): AppSettings {
        return AppSettings(
            playback = PlaybackSettings(
                nasAddress = override.playback.nasAddress.ifBlank { playback.nasAddress },
                nasBaseFolder = override.playback.nasBaseFolder.ifBlank { playback.nasBaseFolder }
            ),
            qrValidation = override.qrValidation.ifBlank { qrValidation },
            mappingFilePostfix = override.mappingFilePostfix.ifBlank { mappingFilePostfix },
            trackIdPadding = if (override.trackIdPadding > 0) override.trackIdPadding else trackIdPadding,
            trackFolderName = override.trackFolderName.ifBlank { trackFolderName },
            smbAuth = SmbAuthSettings(
                username = override.smbAuth.username.ifBlank { smbAuth.username },
                password = override.smbAuth.password.ifBlank { smbAuth.password },
                domain = override.smbAuth.domain.ifBlank { smbAuth.domain }
            )
        )
    }
}

data class AppSettings(
    val playback: PlaybackSettings = PlaybackSettings(nasAddress = "", nasBaseFolder = ""),
    val qrValidation: String = "",
    val mappingFilePostfix: String = "",
    val trackIdPadding: Int = 4,
    val trackFolderName: String = "tracks",
    val smbAuth: SmbAuthSettings = SmbAuthSettings()
)

data class SmbAuthSettings(
    val username: String = "",
    val password: String = "",
    val domain: String = ""
)

private data class SettingsFile(
    val nasAddress: String = "",
    val nasBaseFolder: String = "",
    val qrValidation: String = "",
    val mappingFilePostfix: String = "",
    val trackIdPadding: Int = 0,
    val trackFolderName: String = "",
    val smbUsername: String = "",
    val smbPassword: String = "",
    val smbDomain: String = ""
)

private fun SettingsFile.toAppSettings(): AppSettings {
    return AppSettings(
        playback = PlaybackSettings(
            nasAddress = nasAddress,
            nasBaseFolder = nasBaseFolder
        ),
        qrValidation = qrValidation,
        mappingFilePostfix = mappingFilePostfix,
        trackIdPadding = trackIdPadding,
        trackFolderName = trackFolderName,
        smbAuth = SmbAuthSettings(
            username = smbUsername,
            password = smbPassword,
            domain = smbDomain
        )
    )
}