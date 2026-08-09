package de.kindermaenner.playmymusic.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.playmymusic.offline.OfflineLibrarySyncService
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(
    private val repo: SettingsRepository,
    private val offlineSyncService: OfflineLibrarySyncService
) : ViewModel() {

    val debugMode: StateFlow<Boolean> = repo.debugMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appSettings: StateFlow<AppSettings> = repo.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun toggleDebugMode(value: Boolean) {
        viewModelScope.launch {
            repo.setDebugMode(value)
        }
    }

    fun saveSettings(settings: AppSettings) {
        viewModelScope.launch {
            repo.setAppSettings(settings)
        }
    }

    fun syncMetadata() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = runCatching {
                offlineSyncService.syncMetadata(appSettings.value)
            }.fold(
                onSuccess = { it.message },
                onFailure = { it.message ?: "Metadata sync failed" }
            )
            _isSyncing.value = false
        }
    }

    fun syncTracks() {
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = runCatching {
                offlineSyncService.syncTracks(appSettings.value)
            }.fold(
                onSuccess = { it.message },
                onFailure = { it.message ?: "Track sync failed" }
            )
            _isSyncing.value = false
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }
}
