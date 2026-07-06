package de.kindermaenner.playmymusic.offline

import android.content.Context
import de.kindermaenner.playmymusic.core.model.Song
import de.kindermaenner.playmymusic.settings.AppSettings
import java.io.File

class OfflineLibraryStorage(private val context: Context) {

    private val rootDirectory: File
        get() = File(context.filesDir, "offline-library")

    fun rootDir(): File = rootDirectory

    fun editionsDir(): File = File(rootDirectory, "editions")

    fun editionDir(edition: String): File = File(editionsDir(), sanitize(edition))

    fun localMappingFiles(): List<File> {
        val editionRoots = editionsDir().listFiles()?.filter { it.isDirectory } ?: emptyList()
        return editionRoots.flatMap { editionRoot ->
            editionRoot.listFiles()?.filter { it.isFile && it.extension.equals("json", ignoreCase = true) }
                ?: emptyList()
        }
    }

    fun resolveArtworkSource(song: Song): Any? {
        val imageName = song.image?.trim().orEmpty()
        if (imageName.isBlank()) {
            return null
        }

        val localFile = File(editionDir(song.edition.orEmpty()), imageName)
        return if (localFile.exists()) {
            localFile
        } else {
            "file:///android_asset/$imageName"
        }
    }

    fun resolveLocalTrackFile(song: Song, appSettings: AppSettings): File? {
        val edition = song.edition?.trim().orEmpty()
        if (edition.isBlank()) {
            return null
        }

        val editionDirectory = editionDir(edition)
        val explicitLocation = song.nasLocation.trim()
        if (explicitLocation.isNotBlank()) {
            val candidate = File(editionDirectory, explicitLocation.replace("\\", "/"))
            if (candidate.exists()) {
                return candidate
            }

            val fallbackByName = File(
                File(editionDirectory, appSettings.trackFolderName),
                candidate.name
            )
            if (fallbackByName.exists()) {
                return fallbackByName
            }
        }

        val paddedFileName = song.id.toString()
            .padStart(appSettings.trackIdPadding.coerceAtLeast(1), '0') + ".mp3"
        val generatedFile = File(
            File(editionDirectory, appSettings.trackFolderName.ifBlank { "tracks" }),
            paddedFileName
        )

        return generatedFile.takeIf { it.exists() }
    }

    private fun sanitize(input: String): String {
        return input.trim().replace(Regex("[^A-Za-z0-9._ -]"), "_")
    }
}