package de.kindermaenner.playmymusic.player

import android.net.Uri
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jcifs.CIFSContext
import jcifs.smb.SmbRandomAccessFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.IOException

@OptIn(UnstableApi::class)
@RunWith(RobolectricTestRunner::class)
class SmbDataSourceTest {

    @Test
    fun `buildConfiguredUser handles combinations of user and domain`() {
        val dsNoUser = SmbDataSource("", "pwd", "domain")
        assertEquals("", dsNoUser.buildConfiguredUser())

        val dsUserOnly = SmbDataSource("user", "pwd", "")
        assertEquals("user", dsUserOnly.buildConfiguredUser())

        val dsUserAndDomain = SmbDataSource("user", "pwd", "workgroup")
        assertEquals("workgroup;user", dsUserAndDomain.buildConfiguredUser())
    }

    @Test
    fun `buildCandidateContexts includes anonymous and auth contexts`() {
        val dataSource = SmbDataSource("testUser", "secret", "myDomain")
        val contexts = dataSource.buildCandidateContexts()

        assertTrue(contexts.size >= 4)
    }

    @Test
    fun `open opens file and seeks to position`() {
        val mockRaf = mockk<SmbRandomAccessFile>(relaxed = true)
        every { mockRaf.length() } returns 1000L

        val dataSource = SmbDataSource("u", "p", "d", fileOpener = { _, _ -> mockRaf })
        val testUri = Uri.parse("smb://192.168.1.1/music/song.mp3")
        val dataSpec = DataSpec(testUri, 100L, 500L)

        val bytesRemaining = dataSource.open(dataSpec)

        assertEquals(500L, bytesRemaining)
        assertEquals(testUri, dataSource.uri)
        verify { mockRaf.seek(100L) }
    }

    @Test
    fun `open sets bytesRemaining to full file length when length unset`() {
        val mockRaf = mockk<SmbRandomAccessFile>(relaxed = true)
        every { mockRaf.length() } returns 800L

        val dataSource = SmbDataSource("u", "p", "d", fileOpener = { _, _ -> mockRaf })
        val testUri = Uri.parse("smb://192.168.1.1/music/song.mp3")
        val dataSpec = DataSpec(testUri, 200L, C.LENGTH_UNSET.toLong())

        val bytesRemaining = dataSource.open(dataSpec)

        assertEquals(600L, bytesRemaining)
    }

    @Test
    fun `open throws IOException if requested position is beyond file length`() {
        val mockRaf = mockk<SmbRandomAccessFile>(relaxed = true)
        every { mockRaf.length() } returns 100L

        val dataSource = SmbDataSource("u", "p", "d", fileOpener = { _, _ -> mockRaf })
        val testUri = Uri.parse("smb://192.168.1.1/music/song.mp3")
        val dataSpec = DataSpec(testUri, 150L, 50L)

        try {
            dataSource.open(dataSpec)
            fail("Expected IOException when position is beyond length")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("beyond the SMB file length") == true)
            verify { mockRaf.close() }
        }
    }

    @Test
    fun `open throws IOException when all candidate contexts fail`() {
        val dataSource = SmbDataSource("u", "p", "d", fileOpener = { _, _ ->
            throw IOException("Network timeout")
        })
        val testUri = Uri.parse("smb://192.168.1.1/music/song.mp3")
        val dataSpec = DataSpec(testUri)

        try {
            dataSource.open(dataSpec)
            fail("Expected IOException when all attempts fail")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("SMB open failed for all auth variants") == true)
        }
    }

    @Test
    fun `read returns 0 when length is 0`() {
        val dataSource = SmbDataSource("u", "p", "d")
        val buffer = ByteArray(10)

        val bytesRead = dataSource.read(buffer, 0, 0)
        assertEquals(0, bytesRead)
    }

    @Test
    fun `read returns RESULT_END_OF_INPUT when bytesRemaining is 0`() {
        val mockRaf = mockk<SmbRandomAccessFile>(relaxed = true)
        every { mockRaf.length() } returns 100L

        val dataSource = SmbDataSource("u", "p", "d", fileOpener = { _, _ -> mockRaf })
        val testUri = Uri.parse("smb://192.168.1.1/music/song.mp3")
        dataSource.open(DataSpec.Builder().setUri(testUri).setPosition(100L).build())

        val buffer = ByteArray(10)
        val bytesRead = dataSource.read(buffer, 0, 10)

        assertEquals(C.RESULT_END_OF_INPUT, bytesRead)
    }

    @Test
    fun `read transfers bytes and updates bytesRemaining`() {
        val mockRaf = mockk<SmbRandomAccessFile>(relaxed = true)
        every { mockRaf.length() } returns 100L
        every { mockRaf.read(any(), any(), any()) } answers {
            val buf = firstArg<ByteArray>()
            val off = secondArg<Int>()
            val len = thirdArg<Int>()
            buf[off] = 42
            len
        }

        val dataSource = SmbDataSource("u", "p", "d", fileOpener = { _, _ -> mockRaf })
        val testUri = Uri.parse("smb://192.168.1.1/music/song.mp3")
        dataSource.open(DataSpec(testUri, 0L, 50L))

        val buffer = ByteArray(10)
        val bytesRead = dataSource.read(buffer, 0, 10)

        assertEquals(10, bytesRead)
        assertEquals(42.toByte(), buffer[0])
    }

    @Test
    fun `read returns RESULT_END_OF_INPUT when underlying read returns END_OF_INPUT`() {
        val mockRaf = mockk<SmbRandomAccessFile>(relaxed = true)
        every { mockRaf.length() } returns 100L
        every { mockRaf.read(any(), any(), any()) } returns C.RESULT_END_OF_INPUT

        val dataSource = SmbDataSource("u", "p", "d", fileOpener = { _, _ -> mockRaf })
        val testUri = Uri.parse("smb://192.168.1.1/music/song.mp3")
        dataSource.open(DataSpec(testUri, 0L, 50L))

        val buffer = ByteArray(10)
        val bytesRead = dataSource.read(buffer, 0, 10)

        assertEquals(C.RESULT_END_OF_INPUT, bytesRead)
    }

    @Test
    fun `close resets state and releases file`() {
        val mockRaf = mockk<SmbRandomAccessFile>(relaxed = true)
        every { mockRaf.length() } returns 100L

        val dataSource = SmbDataSource("u", "p", "d", fileOpener = { _, _ -> mockRaf })
        val testUri = Uri.parse("smb://192.168.1.1/music/song.mp3")
        dataSource.open(DataSpec(testUri, 0L, 50L))

        assertNotNull(dataSource.uri)

        dataSource.close()

        assertNull(dataSource.uri)
        verify { mockRaf.close() }
    }

    @Test
    fun `factory creates dataSource`() {
        val factory = SmbDataSource.Factory("usr", "pwd", "dom")
        val ds = factory.createDataSource()

        assertNotNull(ds)
        assertTrue(ds is SmbDataSource)
    }
}
