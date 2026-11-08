package de.kindermaenner.playmymusic.ui.main

import de.kindermaenner.playmymusic.data.mapping.JsonMappingRepository
import de.kindermaenner.playmymusic.domain.model.Track
import de.kindermaenner.playmymusic.domain.usecase.IdentifyTrackUseCase
import de.kindermaenner.playmymusic.domain.usecase.ResolveTrackUriUseCase
import de.kindermaenner.playmymusic.mapping.AssetMappingLoader
import de.kindermaenner.playmymusic.offline.OfflineLibrarySyncService
import de.kindermaenner.playmymusic.offline.SyncResult
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val repo = mockk<SettingsRepository>(relaxed = true)
    private val syncService = mockk<OfflineLibrarySyncService>(relaxed = true)
    private val mappingRepository = mockk<JsonMappingRepository>(relaxed = true)
    private val assetMappingLoader = mockk<AssetMappingLoader>(relaxed = true)
    private val identifyTrackUseCase = mockk<IdentifyTrackUseCase>(relaxed = true)
    private val resolveTrackUriUseCase = mockk<ResolveTrackUriUseCase>(relaxed = true)
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        viewModel = MainViewModel(
            repo = repo,
            offlineSyncService = syncService,
            mappingRepository = mappingRepository,
            assetMappingLoader = assetMappingLoader,
            identifyTrackUseCase = identifyTrackUseCase,
            resolveTrackUriUseCase = resolveTrackUriUseCase
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `onQrScanned updates state with track and uri on success`() {
        val track = Track(1, "Title", "Artist", 2024, "Ed", "E", "url", "img", "")
        every { identifyTrackUseCase(any(), any()) } returns track
        every { resolveTrackUriUseCase(any(), any()) } returns "file://path"

        viewModel.onQrScanned("valid_qr")

        val state = viewModel.uiState.value
        assertEquals(track, state.currentTrack)
        assertEquals("file://path", state.playbackUri)
        assertFalse(state.isScanning)
        assertNull(state.error)
    }

    @Test
    fun `onQrScanned sets error when track not found`() {
        every { identifyTrackUseCase(any(), any()) } returns null

        viewModel.onQrScanned("invalid_qr")

        val state = viewModel.uiState.value
        assertEquals("Invalid QR or Track not found", state.error)
    }

    @Test
    fun `onQrScanned sets error when track uri cannot be resolved`() {
        val track = Track(1, "Title", "Artist", 2024, "Ed", "E", "url", "img", "")
        every { identifyTrackUseCase(any(), any()) } returns track
        every { resolveTrackUriUseCase(any(), any()) } returns null

        viewModel.onQrScanned("valid_qr")

        val state = viewModel.uiState.value
        assertEquals("Could not resolve track URI", state.error)
        assertEquals(track, state.currentTrack)
        assertFalse(state.isScanning)
        assertNull(state.playbackUri)
    }

    @Test
    fun `onPlayPauseToggled updates isPlaying in uiState`() {
        assertFalse(viewModel.uiState.value.isPlaying)

        viewModel.onPlayPauseToggled(true)
        assertTrue(viewModel.uiState.value.isPlaying)

        viewModel.onPlayPauseToggled(false)
        assertFalse(viewModel.uiState.value.isPlaying)
    }

    @Test
    fun `onPlaybackError sets error message in uiState`() {
        viewModel.onPlaybackError("Network playback failed")
        assertEquals("Network playback failed", viewModel.uiState.value.error)
    }

    @Test
    fun `resetToScanner resets scanning state and clears track and error`() {
        val track = Track(1, "Title", "Artist", 2024, "Ed", "E", "url", "img", "")
        every { identifyTrackUseCase(any(), any()) } returns track
        every { resolveTrackUriUseCase(any(), any()) } returns "file://path"
        viewModel.onQrScanned("qr")

        viewModel.resetToScanner()

        val state = viewModel.uiState.value
        assertTrue(state.isScanning)
        assertNull(state.currentTrack)
        assertNull(state.playbackUri)
        assertNull(state.error)
    }

    @Test
    fun `toggleDebugMode updates settings repository`() {
        viewModel.toggleDebugMode(true)
        coVerify { repo.setDebugMode(true) }
    }

    @Test
    fun `saveSettings updates settings repository`() {
        val settings = AppSettings(trackIdPadding = 4)
        viewModel.saveSettings(settings)
        coVerify { repo.setAppSettings(settings) }
    }

    @Test
    fun `loadMappings loads json strings into repository`() {
        every { assetMappingLoader.loadJsonFiles(any()) } returns listOf("{\"meta\":{}}")

        viewModel.loadMappings()

        verify { mappingRepository.load(listOf("{\"meta\":{}}")) }
    }

    @Test
    fun `syncMetadata updates state on success`() {
        coEvery { syncService.syncMetadata(any()) } returns SyncResult("Success")
        
        viewModel.syncMetadata()
        
        assertEquals("Success", viewModel.syncMessage.value)
        assertFalse(viewModel.isSyncing.value)
    }

    @Test
    fun `syncMetadata handles failure and updates state`() {
        coEvery { syncService.syncMetadata(any()) } throws RuntimeException("Connection timeout")

        viewModel.syncMetadata()

        assertEquals("Connection timeout", viewModel.syncMessage.value)
        assertFalse(viewModel.isSyncing.value)
    }

    @Test
    fun `syncTracks updates state on success`() {
        coEvery { syncService.syncTracks(any(), any()) } returns SyncResult("Done")
        
        viewModel.syncTracks()
        
        assertEquals("Done", viewModel.syncMessage.value)
        assertFalse(viewModel.isSyncing.value)
    }

    @Test
    fun `syncTracks handles failure and updates state`() {
        coEvery { syncService.syncTracks(any(), any()) } throws RuntimeException("Disk full")

        viewModel.syncTracks()

        assertEquals("Disk full", viewModel.syncMessage.value)
        assertFalse(viewModel.isSyncing.value)
    }

    @Test
    fun `clearSyncMessage resets message`() {
        coEvery { syncService.syncMetadata(any()) } returns SyncResult("Msg")
        viewModel.syncMetadata()
        
        viewModel.clearSyncMessage()
        
        assertNull(viewModel.syncMessage.value)
    }
}
