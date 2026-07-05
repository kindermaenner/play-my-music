package de.kindermaenner.playmymusic.core.qr

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QrUtilsTest {

    @Test
    fun `isValidQr accepts correct patterns`() {
        assertTrue(isValidQr("AB-1"))
        assertTrue(isValidQr("XYZ-12"))
        assertTrue(isValidQr("A1B2-123"))
        assertTrue(isValidQr("CODE9-7"))
    }

    @Test
    fun `isValidQr rejects invalid patterns`() {
        assertFalse(isValidQr(""))
        assertFalse(isValidQr("A-"))
        assertFalse(isValidQr("-123"))
        assertFalse(isValidQr("ab-12"))      // lowercase not allowed
        assertFalse(isValidQr("ABC_12"))     // wrong separator
        assertFalse(isValidQr("ABC-1234"))   // too many digits
        assertFalse(isValidQr("ABCDEF-12"))  // too many chars before dash
        assertFalse(isValidQr("AB-XYZ"))     // non-numeric second part
    }

    @Test
    fun `parseQr returns null for invalid input`() {
        assertNull(parseQr(""))
        assertNull(parseQr("invalid"))
        assertNull(parseQr("ABC-XYZ"))
        assertNull(parseQr("abc-12"))
        assertNull(parseQr("ABC-1234"))
    }

    @Test
    fun `parseQr splits and pads correctly`() {
        val result1 = parseQr("AB-1")
        assertEquals("AB", result1?.first)
        assertEquals("001", result1?.second)

        val result2 = parseQr("XYZ-12")
        assertEquals("XYZ", result2?.first)
        assertEquals("012", result2?.second)

        val result3 = parseQr("CODE9-123")
        assertEquals("CODE9", result3?.first)
        assertEquals("123", result3?.second)
    }
}