package de.kindermaenner.playmymusic.ui.main

import de.kindermaenner.playmymusic.offline.OfflineLibrarySyncService
import de.kindermaenner.playmymusic.offline.SyncResult
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SettingsRepository
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
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
    private val testDispatcher = UnconfinedTestDispatcher()

    private lateinit var viewModel: MainViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        every { repo.debugMode } returns flowOf(false)
        every { repo.appSettings } returns flowOf(AppSettings())
        viewModel = MainViewModel(repo, syncService)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleDebugMode calls repo`() {
        viewModel.toggleDebugMode(true)
        coVerify { repo.setDebugMode(true) }
    }

    @Test
    fun `saveSettings calls repo`() {
        val settings = AppSettings(qrValidation = "new")
        viewModel.saveSettings(settings)
        coVerify { repo.setAppSettings(settings) }
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
        coEvery { syncService.syncTracks(any()) } returns SyncResult("Done")
        
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
