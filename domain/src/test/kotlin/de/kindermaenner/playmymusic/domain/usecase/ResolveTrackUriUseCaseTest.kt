package de.kindermaenner.playmymusic.domain.usecase

import de.kindermaenner.playmymusic.domain.model.PlaybackConfig
import de.kindermaenner.playmymusic.domain.model.Track
import de.kindermaenner.playmymusic.domain.repository.LocalTrackResolver
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveTrackUriUseCaseTest {

    private class FakeLocalResolver(private val result: String? = null) : LocalTrackResolver {
        override fun resolveLocalUri(track: Track): String? = result
    }

    @Test
    fun `prefer local uri if available`() {
        val useCase = ResolveTrackUriUseCase(FakeLocalResolver("file://local"))
        val track = createDummyTrack()
        val config = PlaybackConfig("nas", "base")

        val result = useCase(track, config)

        assertEquals("file://local", result)
    }

    @Test
    fun `resolve nas path with 4-digit padding`() {
        val useCase = ResolveTrackUriUseCase(FakeLocalResolver(null))
        val track = createDummyTrack(id = 7, edition = "Ed")
        val config = PlaybackConfig(nasAddress = "192.168.1.1", nasBaseFolder = "music", trackIdPadding = 4)

        val result = useCase(track, config)

        assertEquals("smb://192.168.1.1/music/Ed/tracks/0007.mp3", result)
    }

    @Test
    fun `resolve nas path with no padding`() {
        val useCase = ResolveTrackUriUseCase(FakeLocalResolver(null))
        val track = createDummyTrack(id = 7, edition = "Ed")
        val config = PlaybackConfig(nasAddress = "192.168.1.1", nasBaseFolder = "music", trackIdPadding = 1)

        val result = useCase(track, config)

        assertEquals("smb://192.168.1.1/music/Ed/tracks/7.mp3", result)
    }

    @Test
    fun `resolve nas path with explicit nasLocation`() {
        val useCase = ResolveTrackUriUseCase(FakeLocalResolver(null))
        val track = createDummyTrack(id = 1, edition = "Ed").copy(nasLocation = "subdir\\file.mp3")
        val config = PlaybackConfig(nasAddress = "nas.local", nasBaseFolder = "music")

        val result = useCase(track, config)

        assertEquals("smb://nas.local/subdir/file.mp3", result)
    }

    @Test
    fun `resolve nas path handles custom track folder name`() {
        val useCase = ResolveTrackUriUseCase(FakeLocalResolver(null))
        val track = createDummyTrack(id = 1, edition = "Ed")
        val config = PlaybackConfig(nasAddress = "nas", nasBaseFolder = "music", trackFolderName = "songs", trackIdPadding = 2)

        val result = useCase(track, config)

        assertEquals("smb://nas/music/Ed/songs/01.mp3", result)
    }

    @Test
    fun `returns null if nasAddress is blank and local not available`() {
        val useCase = ResolveTrackUriUseCase(FakeLocalResolver(null))
        val track = createDummyTrack()
        val config = PlaybackConfig(nasAddress = "", nasBaseFolder = "music")

        val result = useCase(track, config)

        assertNull(result)
    }

    private fun createDummyTrack(id: Int = 1, edition: String = "E") = Track(
        id = id,
        title = "T",
        artist = "A",
        year = 2024,
        edition = edition,
        editionAbbreviation = "AB",
        urlPart = "url",
        image = "img.jpg",
        nasLocation = ""
    )
}
