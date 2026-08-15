package de.kindermaenner.playmymusic.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.playmymusic.data.mapping.JsonMappingRepository
import de.kindermaenner.playmymusic.domain.model.PlaybackConfig
import de.kindermaenner.playmymusic.domain.usecase.IdentifyTrackUseCase
import de.kindermaenner.playmymusic.domain.usecase.ResolveTrackUriUseCase
import de.kindermaenner.playmymusic.mapping.AssetMappingLoader
import de.kindermaenner.playmymusic.offline.OfflineLibrarySyncService
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(
    private val repo: SettingsRepository,
    private val offlineSyncService: OfflineLibrarySyncService,
    private val mappingRepository: JsonMappingRepository,
    private val assetMappingLoader: AssetMappingLoader,
    private val identifyTrackUseCase: IdentifyTrackUseCase,
    private val resolveTrackUriUseCase: ResolveTrackUriUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    val debugMode: StateFlow<Boolean> = repo.debugMode
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val appSettings: StateFlow<AppSettings> = repo.appSettings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppSettings())

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    fun onQrScanned(qrText: String) {
        val settings = appSettings.value
        val track = identifyTrackUseCase(qrText, settings.qrValidation)
        
        if (track == null) {
            _uiState.update { it.copy(error = "Invalid QR or Track not found") }
            return
        }

        val config = PlaybackConfig(
            nasAddress = settings.playback.nasAddress,
            nasBaseFolder = settings.playback.nasBaseFolder,
            trackIdPadding = settings.trackIdPadding,
            trackFolderName = settings.trackFolderName
        )

        val uri = resolveTrackUriUseCase(track, config)
        if (uri == null) {
            _uiState.update { it.copy(error = "Could not resolve track URI", currentTrack = track, isScanning = false) }
        } else {
            _uiState.update { it.copy(currentTrack = track, playbackUri = uri, error = null, isScanning = false) }
        }
    }

    fun onPlayPauseToggled(playing: Boolean) {
        _uiState.update { it.copy(isPlaying = playing) }
    }

    fun onPlaybackError(message: String) {
        _uiState.update { it.copy(error = message) }
    }

    fun resetToScanner() {
        _uiState.update { it.copy(isScanning = true, currentTrack = null, playbackUri = null, error = null) }
    }

    fun loadMappings() {
        viewModelScope.launch {
            val jsonStrings = assetMappingLoader.loadJsonFiles(appSettings.value.mappingFilePostfix)
            mappingRepository.load(jsonStrings)
        }
    }

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
                onSuccess = { 
                    loadMappings()
                    it.message 
                },
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
