package de.kindermaenner.playmymusic.domain.usecase

import de.kindermaenner.playmymusic.domain.model.PlaybackConfig
import de.kindermaenner.playmymusic.domain.model.Track
import de.kindermaenner.playmymusic.domain.repository.LocalTrackResolver

class ResolveTrackUriUseCase(
    private val localResolver: LocalTrackResolver
) {
    operator fun invoke(track: Track, config: PlaybackConfig): String? {
        // 1. Check local storage first (Offline-First)
        localResolver.resolveLocalUri(track)?.let { return it }

        // 2. Fallback to NAS path resolution
        val resolvedPath = resolveNasPath(config, track) ?: return null
        return toPlayableUri(resolvedPath)
    }

    private fun resolveNasPath(config: PlaybackConfig, track: Track): String? {
        val nasAddress = normalizeNasAddress(config.nasAddress)
        if (nasAddress.isBlank()) return null

        val separator = if (nasAddress.contains("://")) "/" else "\\"

        return if (track.nasLocation.isNotBlank()) {
            join(separator, nasAddress, track.nasLocation)
        } else {
            val edition = track.edition.trim()
            val nasBaseFolder = config.nasBaseFolder.trim()

            if (nasBaseFolder.isBlank() || edition.isBlank()) return null

            val fileName = track.id.toString().padStart(config.trackIdPadding.coerceAtLeast(1), '0') + ".mp3"
            join(separator, nasAddress, nasBaseFolder, edition, config.trackFolderName.ifBlank { "tracks" }, fileName)
        }
    }

    private fun normalizeNasAddress(rawAddress: String): String {
        val trimmed = rawAddress.trim()
        if (trimmed.isBlank() || trimmed.contains("://")) return trimmed
        return "\\\\${trimmed.trimStart('\\', '/')}"
    }

    private fun join(separator: String, vararg parts: String): String {
        return parts.filter { it.isNotBlank() }.mapIndexed { index, value ->
            val normalized = value.replace("\\", separator).replace("/", separator)
            if (index == 0) normalized.trimEnd(separator[0]) else normalized.trim(separator[0])
        }.joinToString(separator)
    }

    private fun toPlayableUri(path: String): String? {
        val trimmed = path.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.contains("://")) return trimmed.replace("\\", "/")

        val normalized = trimmed.replace(Regex("[\\t\\n\\r\\u000C]"), "\\\\").replace("/", "\\")
        val segments = normalized.trimStart('\\').split('\\').filter { it.isNotBlank() }
        if (segments.size < 2) return null

        return "smb://${segments.joinToString("/")}"
    }
}
