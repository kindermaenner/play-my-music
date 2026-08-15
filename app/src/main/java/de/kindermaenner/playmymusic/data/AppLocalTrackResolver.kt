package de.kindermaenner.playmymusic.data

import de.kindermaenner.playmymusic.domain.model.Track
import de.kindermaenner.playmymusic.domain.repository.LocalTrackResolver
import de.kindermaenner.playmymusic.offline.OfflineLibraryStorage
import java.io.File

class AppLocalTrackResolver(
    private val storage: OfflineLibraryStorage,
    private val trackIdPadding: Int,
    private val trackFolderName: String
) : LocalTrackResolver {
    override fun resolveLocalUri(track: Track): String? {
        val edition = track.edition.trim()
        if (edition.isBlank()) return null

        val editionDirectory = storage.editionDir(edition)
        
        // 1. Check explicit location
        val explicitLocation = track.nasLocation.trim()
        if (explicitLocation.isNotBlank()) {
            val candidate = File(editionDirectory, explicitLocation.replace("\\", "/"))
            if (candidate.exists()) return candidate.toURI().toString()

            val fallbackByName = File(File(editionDirectory, trackFolderName), candidate.name)
            if (fallbackByName.exists()) return fallbackByName.toURI().toString()
        }

        // 2. Check standard location
        val paddedFileName = track.id.toString().padStart(trackIdPadding.coerceAtLeast(1), '0') + ".mp3"
        val generatedFile = File(File(editionDirectory, trackFolderName.ifBlank { "tracks" }), paddedFileName)

        return if (generatedFile.exists()) generatedFile.toURI().toString() else null
    }
}
