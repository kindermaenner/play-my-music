package de.kindermaenner.playmymusic.core.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrUtilsTest {

    private val validationPrefix = "example.org/game/de/"

    @Test
    fun `isValidQr accepts correct patterns`() {
        assertTrue(isValidQr("http://www.example.org/game/de/00009", validationPrefix))
        assertTrue(isValidQr("https://www.example.org/game/de/edition1234/00086", validationPrefix))
        assertTrue(isValidQr("https://example.org/game/de/00042", validationPrefix))
        assertTrue(isValidQr("www.example.org/game/de/00001", validationPrefix))
    }

    @Test
    fun `isValidQr rejects invalid patterns`() {
        assertFalse(isValidQr("", validationPrefix))
        assertFalse(isValidQr("http://example.com/de/00009", validationPrefix))
        assertFalse(isValidQr("http://www.example.org/game/en/00009", validationPrefix))
        assertFalse(isValidQr("http://www.example.org/game/de/edition1234/not-a-number", validationPrefix))
        assertFalse(isValidQr("not-an-url", validationPrefix))
    }

    @Test
    fun `parseQr returns null for invalid input`() {
        assertNull(parseQr("", validationPrefix))
        assertNull(parseQr("invalid", validationPrefix))
        assertNull(parseQr("http://www.example.org/game/en/00009", validationPrefix))
        assertNull(parseQr("http://www.example.org/game/de/edition1234/not-a-number", validationPrefix))
    }

    @Test
    fun `parseQr extracts edition path and track id`() {
        val baseEdition = parseQr("http://www.example.org/game/de/00009", validationPrefix)
        assertEquals("", baseEdition?.urlPart)
        assertEquals(9, baseEdition?.trackId)

        val specialEdition = parseQr("https://www.example.org/game/de/edition1234/00086", validationPrefix)
        assertEquals("edition1234", specialEdition?.urlPart)
        assertEquals(86, specialEdition?.trackId)

        val noScheme = parseQr("www.example.org/game/de/00001", validationPrefix)
        assertEquals("", noScheme?.urlPart)
        assertEquals(1, noScheme?.trackId)
    }
}
