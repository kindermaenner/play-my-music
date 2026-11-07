package de.kindermaenner.playmymusic.data

import de.kindermaenner.playmymusic.domain.model.Track
import de.kindermaenner.playmymusic.domain.repository.LocalTrackResolver
import de.kindermaenner.playmymusic.offline.OfflineLibraryStorage
import de.kindermaenner.playmymusic.settings.AppSettings
import java.io.File

class AppLocalTrackResolver(
    private val storage: OfflineLibraryStorage,
    private val settingsProvider: () -> AppSettings
) : LocalTrackResolver {

    constructor(
        storage: OfflineLibraryStorage,
        trackIdPadding: Int,
        trackFolderName: String
    ) : this(
        storage,
        { AppSettings(trackIdPadding = trackIdPadding, trackFolderName = trackFolderName) }
    )

    override fun resolveLocalUri(track: Track): String? {
        val edition = track.edition.trim()
        if (edition.isBlank()) return null

        val appSettings = settingsProvider()
        val editionDirectory = storage.editionDir(edition)
        val trackFolder = appSettings.trackFolderName.ifBlank { "tracks" }

        // 1. Check explicit location
        val explicitLocation = track.nasLocation.trim()
        if (explicitLocation.isNotBlank()) {
            val candidate = File(editionDirectory, explicitLocation.replace("\\", "/"))
            if (candidate.exists()) return candidate.toURI().toString()

            val fallbackByName = File(File(editionDirectory, trackFolder), candidate.name)
            if (fallbackByName.exists()) return fallbackByName.toURI().toString()
        }

        // 2. Check standard location
        val paddedFileName = track.id.toString()
            .padStart(appSettings.trackIdPadding.coerceAtLeast(1), '0') + ".mp3"
        val generatedFile = File(File(editionDirectory, trackFolder), paddedFileName)

        return if (generatedFile.exists()) generatedFile.toURI().toString() else null
    }
}
