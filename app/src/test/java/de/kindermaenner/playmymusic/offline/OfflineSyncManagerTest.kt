package de.kindermaenner.playmymusic.offline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OfflineSyncManagerTest {

    @Before
    fun setup() {
        OfflineSyncManager.setSyncing(false)
        OfflineSyncManager.updateProgress(null)
        OfflineSyncManager.clearMessage()
    }

    @Test
    fun `setSyncing updates isSyncing flow`() {
        assertFalse(OfflineSyncManager.isSyncing.value)

        OfflineSyncManager.setSyncing(true)
        assertTrue(OfflineSyncManager.isSyncing.value)

        OfflineSyncManager.setSyncing(false)
        assertFalse(OfflineSyncManager.isSyncing.value)
    }

    @Test
    fun `updateProgress updates syncProgress flow`() {
        assertNull(OfflineSyncManager.syncProgress.value)

        val progress = SyncProgress(current = 5, total = 20, currentItem = "track.mp3")
        OfflineSyncManager.updateProgress(progress)

        assertEquals(progress, OfflineSyncManager.syncProgress.value)

        OfflineSyncManager.updateProgress(null)
        assertNull(OfflineSyncManager.syncProgress.value)
    }

    @Test
    fun `setMessage and clearMessage manage syncMessage flow`() {
        assertNull(OfflineSyncManager.syncMessage.value)

        OfflineSyncManager.setMessage("Sync complete")
        assertEquals("Sync complete", OfflineSyncManager.syncMessage.value)

        OfflineSyncManager.clearMessage()
        assertNull(OfflineSyncManager.syncMessage.value)
    }
}
