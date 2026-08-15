package de.kindermaenner.playmymusic.domain.usecase

import de.kindermaenner.playmymusic.domain.model.Track
import de.kindermaenner.playmymusic.domain.repository.MappingRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class IdentifyTrackUseCaseTest {

    private class FakeRepository : MappingRepository {
        override fun getTrack(urlPart: String, id: Int): Track? {
            if (id == 42) return Track(42, "T", "A", 2024, "E", "EA", urlPart, "I", "")
            return null
        }
    }

    private val useCase = IdentifyTrackUseCase(FakeRepository())

    @Test
    fun `identifies track with full https url and prefix`() {
        val result = useCase("https://example.com/p1/42", "example.com/")
        assertEquals(42, result?.id)
        assertEquals("p1", result?.urlPart)
    }

    @Test
    fun `identifies track with full http url and prefix`() {
        val result = useCase("http://example.com/p1/42", "example.com/")
        assertEquals(42, result?.id)
        assertEquals("p1", result?.urlPart)
    }

    @Test
    fun `identifies track with no protocol and prefix`() {
        val result = useCase("example.com/p1/42", "example.com/")
        assertEquals(42, result?.id)
        assertEquals("p1", result?.urlPart)
    }

    @Test
    fun `identifies track with only ID and no prefix`() {
        val result = useCase("42", "")
        assertEquals(42, result?.id)
        assertEquals("", result?.urlPart)
    }

    @Test
    fun `identifies track with url part and ID and no prefix`() {
        val result = useCase("p1/42", "")
        assertEquals(42, result?.id)
        assertEquals("p1", result?.urlPart)
    }

    @Test
    fun `identifies track with full url and no prefix`() {
        val result = useCase("https://example.com/p1/42", "")
        assertEquals(42, result?.id)
        assertEquals("p1", result?.urlPart)
    }

    @Test
    fun `identifies track with query parameters`() {
        val result = useCase("https://example.com/p1/42?foo=bar", "example.com/")
        assertEquals(42, result?.id)
        assertEquals("p1", result?.urlPart)
    }

    @Test
    fun `returns null if prefix not found`() {
        val result = useCase("https://other.com/p1/42", "example.com/")
        assertNull(result)
    }

    @Test
    fun `returns null for empty input`() {
        assertNull(useCase("", ""))
        assertNull(useCase("   ", ""))
    }

    @Test
    fun `returns null for non-numeric track id`() {
        assertNull(useCase("p1/abc", ""))
    }

    @Test
    fun `returns null for unknown track`() {
        val result = useCase("https://example.com/p1/99", "example.com/")
        assertNull(result)
    }
}
