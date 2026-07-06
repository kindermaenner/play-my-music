package de.kindermaenner.playmymusic.core.mapping

import de.kindermaenner.playmymusic.core.model.PlaybackSettings
import de.kindermaenner.playmymusic.core.model.Song

object TrackPathResolver {

    fun resolve(
        settings: PlaybackSettings,
        song: Song,
        trackIdPadding: Int = 4,
        trackFolderName: String = "tracks"
    ): String? {
        val nasAddress = normalizeNasAddress(settings.nasAddress)
        if (nasAddress.isBlank()) {
            return null
        }

        val separator = if (nasAddress.contains("://")) "/" else "\\"

        return if (song.nasLocation.isNotBlank()) {
            join(separator, nasAddress, song.nasLocation)
        } else {
            val edition = song.edition?.trim().orEmpty()
            val nasBaseFolder = settings.nasBaseFolder.trim()

            if (nasBaseFolder.isBlank() || edition.isBlank()) {
                return null
            }

            val normalizedPadding = trackIdPadding.coerceAtLeast(1)
            val normalizedTrackFolderName = trackFolderName.trim().ifBlank { "tracks" }
            val fileName = song.id.toString().padStart(normalizedPadding, '0') + ".mp3"
            join(separator, nasAddress, nasBaseFolder, edition, normalizedTrackFolderName, fileName)
        }
    }

    private fun normalizeNasAddress(rawAddress: String): String {
        val trimmedAddress = rawAddress.trim()
        if (trimmedAddress.isBlank()) {
            return ""
        }

        if (trimmedAddress.contains("://")) {
            return trimmedAddress
        }

        val hostPart = trimmedAddress.trimStart('\\', '/')
        return "\\\\$hostPart"
    }

    private fun join(separator: String, vararg parts: String): String {
        return parts
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapIndexed { index, value ->
                val normalized = value.replace("\\", separator).replace("/", separator)
                when {
                    index == 0 && separator == "\\" -> normalized.trimEnd('\\')
                    index == 0 -> normalized.trimEnd('/')
                    separator == "\\" -> normalized.trim('\\')
                    else -> normalized.trim('/')
                }
            }
            .joinToString(separator)
    }
}