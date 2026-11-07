package de.kindermaenner.playmymusic.offline

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object OfflineSyncManager {
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing = _isSyncing.asStateFlow()

    private val _syncProgress = MutableStateFlow<SyncProgress?>(null)
    val syncProgress = _syncProgress.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage = _syncMessage.asStateFlow()

    fun setSyncing(syncing: Boolean) {
        _isSyncing.value = syncing
    }

    fun updateProgress(progress: SyncProgress?) {
        _syncProgress.value = progress
    }

    fun setMessage(message: String?) {
        _syncMessage.value = message
    }

    fun clearMessage() {
        _syncMessage.value = null
    }
}
