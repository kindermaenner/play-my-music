package de.kindermaenner.playmymusic.ui.main

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import de.kindermaenner.playmymusic.data.mapping.JsonMappingRepository
import de.kindermaenner.playmymusic.domain.model.PlaybackConfig
import de.kindermaenner.playmymusic.domain.usecase.IdentifyTrackUseCase
import de.kindermaenner.playmymusic.domain.usecase.ResolveTrackUriUseCase
import de.kindermaenner.playmymusic.mapping.AssetMappingLoader
import de.kindermaenner.playmymusic.offline.OfflineLibrarySyncService
import de.kindermaenner.playmymusic.offline.OfflineSyncManager
import de.kindermaenner.playmymusic.offline.SyncForegroundService
import de.kindermaenner.playmymusic.offline.SyncProgress
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

    val syncMessage: StateFlow<String?> = OfflineSyncManager.syncMessage
    val syncProgress: StateFlow<SyncProgress?> = OfflineSyncManager.syncProgress
    val isSyncing: StateFlow<Boolean> = OfflineSyncManager.isSyncing

    init {
        viewModelScope.launch {
            OfflineSyncManager.isSyncing.collect { syncing ->
                if (!syncing && OfflineSyncManager.syncMessage.value != null) {
                    loadMappings()
                }
            }
        }
    }

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

    fun syncMetadata(context: Context? = null) {
        if (context != null) {
            SyncForegroundService.startSync(context, appSettings.value, includeTracks = false)
        } else {
            viewModelScope.launch {
                OfflineSyncManager.setSyncing(true)
                OfflineSyncManager.clearMessage()
                val result = runCatching {
                    offlineSyncService.syncMetadata(appSettings.value)
                }
                OfflineSyncManager.setSyncing(false)
                result.fold(
                    onSuccess = {
                        loadMappings()
                        OfflineSyncManager.setMessage(it.message)
                    },
                    onFailure = {
                        OfflineSyncManager.setMessage(it.message ?: "Metadata sync failed")
                    }
                )
            }
        }
    }

    fun syncTracks(context: Context? = null) {
        if (context != null) {
            SyncForegroundService.startSync(context, appSettings.value, includeTracks = true)
        } else {
            viewModelScope.launch {
                OfflineSyncManager.setSyncing(true)
                OfflineSyncManager.clearMessage()
                val result = runCatching {
                    offlineSyncService.syncTracks(appSettings.value) {
                        OfflineSyncManager.updateProgress(it)
                    }
                }
                OfflineSyncManager.setSyncing(false)
                OfflineSyncManager.updateProgress(null)
                result.fold(
                    onSuccess = {
                        loadMappings()
                        OfflineSyncManager.setMessage(it.message)
                    },
                    onFailure = {
                        OfflineSyncManager.setMessage(it.message ?: "Track sync failed")
                    }
                )
            }
        }
    }

    fun clearSyncMessage() {
        OfflineSyncManager.clearMessage()
    }
}
