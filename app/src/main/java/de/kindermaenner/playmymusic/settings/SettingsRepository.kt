package de.kindermaenner.playmymusic.settings

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import de.kindermaenner.playmymusic.core.model.PlaybackSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore("settings")

object SettingsKeys {
    val DEBUG_MODE = booleanPreferencesKey("debug_mode")
    val NAS_ADDRESS = stringPreferencesKey("nas_address")
    val NAS_BASE_FOLDER = stringPreferencesKey("nas_base_folder")
    val QR_VALIDATION = stringPreferencesKey("qr_validation")
    val MAPPING_FILE_POSTFIX = stringPreferencesKey("mapping_file_postfix")
    val TRACK_ID_PADDING = intPreferencesKey("track_id_padding")
    val TRACK_FOLDER_NAME = stringPreferencesKey("track_folder_name")
    val SMB_USERNAME = stringPreferencesKey("smb_username")
    val SMB_PASSWORD = stringPreferencesKey("smb_password")
    val SMB_DOMAIN = stringPreferencesKey("smb_domain")
}

class SettingsRepository(private val context: Context) {

    private val defaultSettings by lazy { Settings.defaultAppSettings(context) }

    val debugMode: Flow<Boolean> =
        context.settingsDataStore.data.map { it[SettingsKeys.DEBUG_MODE] ?: false }

    val appSettings: Flow<AppSettings> =
        context.settingsDataStore.data.map { preferences ->
            AppSettings(
                playback = PlaybackSettings(
                    nasAddress = preferences[SettingsKeys.NAS_ADDRESS]
                        ?: defaultSettings.playback.nasAddress,
                    nasBaseFolder = preferences[SettingsKeys.NAS_BASE_FOLDER]
                        ?: defaultSettings.playback.nasBaseFolder
                ),
                qrValidation = preferences[SettingsKeys.QR_VALIDATION]
                    ?: defaultSettings.qrValidation,
                mappingFilePostfix = preferences[SettingsKeys.MAPPING_FILE_POSTFIX]
                    ?: defaultSettings.mappingFilePostfix,
                trackIdPadding = preferences[SettingsKeys.TRACK_ID_PADDING]
                    ?: defaultSettings.trackIdPadding,
                trackFolderName = preferences[SettingsKeys.TRACK_FOLDER_NAME]
                    ?: defaultSettings.trackFolderName,
                smbAuth = SmbAuthSettings(
                    username = preferences[SettingsKeys.SMB_USERNAME]
                        ?: defaultSettings.smbAuth.username,
                    password = preferences[SettingsKeys.SMB_PASSWORD]
                        ?: defaultSettings.smbAuth.password,
                    domain = preferences[SettingsKeys.SMB_DOMAIN]
                        ?: defaultSettings.smbAuth.domain
                )
            )
        }

    suspend fun setDebugMode(value: Boolean) {
        context.settingsDataStore.edit { it[SettingsKeys.DEBUG_MODE] = value }
    }

    suspend fun setAppSettings(settings: AppSettings) {
        context.settingsDataStore.edit { preferences ->
            preferences[SettingsKeys.NAS_ADDRESS] = settings.playback.nasAddress
            preferences[SettingsKeys.NAS_BASE_FOLDER] = settings.playback.nasBaseFolder
            preferences[SettingsKeys.QR_VALIDATION] = settings.qrValidation
            preferences[SettingsKeys.MAPPING_FILE_POSTFIX] = settings.mappingFilePostfix
            preferences[SettingsKeys.TRACK_ID_PADDING] = settings.trackIdPadding
            preferences[SettingsKeys.TRACK_FOLDER_NAME] = settings.trackFolderName
            preferences[SettingsKeys.SMB_USERNAME] = settings.smbAuth.username
            preferences[SettingsKeys.SMB_PASSWORD] = settings.smbAuth.password
            preferences[SettingsKeys.SMB_DOMAIN] = settings.smbAuth.domain
        }
    }
}