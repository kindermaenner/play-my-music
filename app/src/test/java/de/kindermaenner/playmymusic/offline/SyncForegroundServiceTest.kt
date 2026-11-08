package de.kindermaenner.playmymusic.offline

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import com.google.gson.Gson
import de.kindermaenner.playmymusic.settings.AppSettings
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
@OptIn(ExperimentalCoroutinesApi::class)
class SyncForegroundServiceTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        OfflineSyncManager.setSyncing(false)
        OfflineSyncManager.updateProgress(null)
        OfflineSyncManager.clearMessage()
    }

    @After
    fun tearDown() {
        OfflineSyncManager.setSyncing(false)
        OfflineSyncManager.updateProgress(null)
        OfflineSyncManager.clearMessage()
    }

    @Test
    fun `onBind returns null`() {
        val controller = Robolectric.buildService(SyncForegroundService::class.java)
        val service = controller.create().get()

        assertNull(service.onBind(null))
    }

    @Test
    fun `startSync puts correct extras in intent`() {
        val settings = AppSettings()
        SyncForegroundService.startSync(context, settings, includeTracks = true)

        val shadowApp = shadowOf(context as android.app.Application)
        val nextServiceIntent = shadowApp.nextStartedService
        assertNotNull(nextServiceIntent)
        assertEquals(SyncForegroundService::class.java.name, nextServiceIntent.component?.className)
        assertTrue(nextServiceIntent.getBooleanExtra(SyncForegroundService.EXTRA_INCLUDE_TRACKS, false))
        val json = nextServiceIntent.getStringExtra(SyncForegroundService.EXTRA_SETTINGS_JSON)
        assertNotNull(json)
        val parsed = Gson().fromJson(json, AppSettings::class.java)
        assertEquals(settings, parsed)
    }

    @Test
    fun `onStartCommand with includeTracks true runs syncTracks and reports success`() = runTest {
        val mockSyncService = mockk<OfflineLibrarySyncService>()
        val expectedResult = SyncResult("Synchronisation erfolgreich", 2, 2, 50, 50)

        coEvery {
            mockSyncService.syncTracks(any(), any())
        } answers {
            val progressCallback = secondArg<((SyncProgress) -> Unit)?>()
            progressCallback?.invoke(SyncProgress(current = 1, total = 50, currentItem = "track01.mp3"))
            expectedResult
        }

        val controller = Robolectric.buildService(SyncForegroundService::class.java)
        val service = controller.get()
        service.ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        service.syncService = mockSyncService
        controller.create()

        val intent = Intent(context, SyncForegroundService::class.java).apply {
            putExtra(SyncForegroundService.EXTRA_SETTINGS_JSON, Gson().toJson(AppSettings()))
            putExtra(SyncForegroundService.EXTRA_INCLUDE_TRACKS, true)
        }

        service.onStartCommand(intent, 0, 1)

        assertFalse(OfflineSyncManager.isSyncing.value)
        assertEquals("Synchronisation erfolgreich", OfflineSyncManager.syncMessage.value)

        val shadowService = shadowOf(service)
        assertTrue(shadowService.isStoppedBySelf)
        assertTrue(shadowService.isForegroundStopped)
    }

    @Test
    fun `onStartCommand with includeTracks false runs syncMetadata and reports success`() = runTest {
        val mockSyncService = mockk<OfflineLibrarySyncService>()
        val expectedResult = SyncResult("Metadata synced", 2, 2, 0, 0)

        coEvery {
            mockSyncService.syncMetadata(any())
        } returns expectedResult

        val controller = Robolectric.buildService(SyncForegroundService::class.java)
        val service = controller.get()
        service.ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        service.syncService = mockSyncService
        controller.create()

        val intent = Intent(context, SyncForegroundService::class.java).apply {
            putExtra(SyncForegroundService.EXTRA_SETTINGS_JSON, Gson().toJson(AppSettings()))
            putExtra(SyncForegroundService.EXTRA_INCLUDE_TRACKS, false)
        }

        service.onStartCommand(intent, 0, 1)

        assertFalse(OfflineSyncManager.isSyncing.value)
        assertEquals("Metadata synced", OfflineSyncManager.syncMessage.value)
    }

    @Test
    fun `onStartCommand failure posts error message to OfflineSyncManager`() = runTest {
        val mockSyncService = mockk<OfflineLibrarySyncService>()

        coEvery {
            mockSyncService.syncTracks(any(), any())
        } throws RuntimeException("NAS Verbindung fehlgeschlagen")

        val controller = Robolectric.buildService(SyncForegroundService::class.java)
        val service = controller.get()
        service.ioDispatcher = kotlinx.coroutines.Dispatchers.Unconfined
        service.syncService = mockSyncService
        controller.create()

        val intent = Intent(context, SyncForegroundService::class.java).apply {
            putExtra(SyncForegroundService.EXTRA_SETTINGS_JSON, Gson().toJson(AppSettings()))
            putExtra(SyncForegroundService.EXTRA_INCLUDE_TRACKS, true)
        }

        service.onStartCommand(intent, 0, 1)

        assertFalse(OfflineSyncManager.isSyncing.value)
        assertEquals("NAS Verbindung fehlgeschlagen", OfflineSyncManager.syncMessage.value)
    }

    @Test
    fun `onDestroy resets syncing state`() {
        val controller = Robolectric.buildService(SyncForegroundService::class.java)
        val service = controller.create().get()

        OfflineSyncManager.setSyncing(true)
        OfflineSyncManager.updateProgress(SyncProgress(1, 10, "track.mp3"))

        controller.destroy()

        assertFalse(OfflineSyncManager.isSyncing.value)
        assertNull(OfflineSyncManager.syncProgress.value)
    }
}
