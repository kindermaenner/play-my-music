package de.kindermaenner.playmymusic.ui.main

import de.kindermaenner.playmymusic.data.mapping.JsonMappingRepository
import de.kindermaenner.playmymusic.domain.model.Track
import de.kindermaenner.playmymusic.domain.usecase.IdentifyTrackUseCase
import de.kindermaenner.playmymusic.domain.usecase.ResolveTrackUriUseCase
import de.kindermaenner.playmymusic.mapping.AssetMappingLoader
import de.kindermaenner.playmymusic.offline.OfflineLibrarySyncService
import de.kindermaenner.playmymusic.offline.SyncResult
import de.kindermaenner.playmymusic.settings.SettingsRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
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
        assertEquals(false, state.isScanning)
        assertEquals(null, state.error)
    }

    @Test
    fun `onQrScanned sets error when track not found`() {
        every { identifyTrackUseCase(any(), any()) } returns null

        viewModel.onQrScanned("invalid_qr")

        val state = viewModel.uiState.value
        assertEquals("Invalid QR or Track not found", state.error)
    }

    @Test
    fun `syncMetadata updates state correctly`() {
        coEvery { syncService.syncMetadata(any()) } returns SyncResult("Success")
        
        viewModel.syncMetadata()
        
        assertEquals("Success", viewModel.syncMessage.value)
        assertEquals(false, viewModel.isSyncing.value)
    }

    @Test
    fun `syncTracks updates state correctly`() {
        coEvery { syncService.syncTracks(any(), any()) } returns SyncResult("Done")
        
        viewModel.syncTracks()
        
        assertEquals("Done", viewModel.syncMessage.value)
        assertEquals(false, viewModel.isSyncing.value)
    }

    @Test
    fun `clearSyncMessage resets message`() {
        coEvery { syncService.syncMetadata(any()) } returns SyncResult("Msg")
        viewModel.syncMetadata()
        
        viewModel.clearSyncMessage()
        
        assertEquals(null, viewModel.syncMessage.value)
    }
}
