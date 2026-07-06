package de.kindermaenner.playmymusic.offline

import android.content.Context
import com.google.gson.Gson
import de.kindermaenner.playmymusic.core.model.MappingFile
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SmbAuthSettings
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class SyncResult(
    val message: String,
    val copiedMappings: Int = 0,
    val copiedImages: Int = 0,
    val copiedTracks: Int = 0
)

class OfflineLibrarySyncService(private val context: Context) {

    private val gson = Gson()
    private val storage = OfflineLibraryStorage(context)

    suspend fun syncMetadata(settings: AppSettings): SyncResult {
        return withContext(Dispatchers.IO) {
            sync(settings = settings, includeTracks = false)
        }
    }

    suspend fun syncTracks(settings: AppSettings): SyncResult {
        return withContext(Dispatchers.IO) {
            sync(settings = settings, includeTracks = true)
        }
    }

    private fun sync(settings: AppSettings, includeTracks: Boolean): SyncResult {
        val root = resolveBaseDirectory(settings)
        val editionDirectories = root.listFiles().orEmpty().filter { it.isDirectory }

        var copiedMappings = 0
        var copiedImages = 0
        var copiedTracks = 0

        editionDirectories.forEach { editionDirectory ->
            val targetEditionDirectory = storage.editionDir(editionDirectory.name).apply { mkdirs() }
            val mappingFile = editionDirectory.listFiles().orEmpty()
                .firstOrNull { it.isFile && it.name.endsWith(".json", ignoreCase = true) }
                ?: return@forEach

            val localMappingFile = File(targetEditionDirectory, mappingFile.name)
            copySmbFile(mappingFile, localMappingFile)
            copiedMappings += 1

            val mapping = gson.fromJson(localMappingFile.readText(), MappingFile::class.java)
            val imageName = mapping.meta.image.trim()
            if (imageName.isNotBlank()) {
                val imageSource = SmbFile(editionDirectory, imageName)
                if (imageSource.exists()) {
                    copySmbFile(imageSource, File(targetEditionDirectory, imageName))
                    copiedImages += 1
                }
            }

            if (includeTracks) {
                val tracksFolderName = settings.trackFolderName.ifBlank { "tracks" }
                val remoteTracksDirectory = SmbFile(editionDirectory, "$tracksFolderName/")
                if (remoteTracksDirectory.exists() && remoteTracksDirectory.isDirectory) {
                    copiedTracks += copyDirectoryRecursive(
                        sourceDirectory = remoteTracksDirectory,
                        targetDirectory = File(targetEditionDirectory, tracksFolderName)
                    )
                }
            }
        }

        val message = if (includeTracks) {
            "Synced $copiedMappings mapping files, $copiedImages images, and $copiedTracks tracks."
        } else {
            "Synced $copiedMappings mapping files and $copiedImages images."
        }

        return SyncResult(
            message = message,
            copiedMappings = copiedMappings,
            copiedImages = copiedImages,
            copiedTracks = copiedTracks
        )
    }

    private fun resolveBaseDirectory(settings: AppSettings): SmbFile {
        val baseUri = buildBaseUri(settings)
        val contexts = buildCandidateContexts(settings.smbAuth)
        var lastError: Exception? = null

        for (context in contexts) {
            try {
                val baseDirectory = SmbFile(baseUri, context)
                if (baseDirectory.exists() && baseDirectory.isDirectory) {
                    return baseDirectory
                }
            } catch (error: Exception) {
                lastError = error
            }
        }

        val detail = lastError?.message ?: "unknown error"
        throw IOException("Unable to access NAS base directory: $detail", lastError)
    }

    private fun buildBaseUri(settings: AppSettings): String {
        val host = settings.playback.nasAddress.trim().trimStart('\\', '/')
        val baseFolder = settings.playback.nasBaseFolder.trim().replace('\\', '/')
        return if (baseFolder.isBlank()) {
            "smb://$host/"
        } else {
            "smb://$host/${baseFolder.trim('/')}/"
        }
    }

    private fun buildCandidateContexts(authSettings: SmbAuthSettings): List<CIFSContext> {
        val baseContext = SingletonContext.getInstance()
        val contexts = mutableListOf<CIFSContext>()
        contexts.add(baseContext.withCredentials(NtlmPasswordAuthenticator("", "")))
        contexts.add(baseContext.withCredentials(NtlmPasswordAuthenticator("guest", "")))
        contexts.add(baseContext.withCredentials(NtlmPasswordAuthenticator("anonymous", "")))

        val configuredUser = authSettings.username.trim()
        if (configuredUser.isNotBlank()) {
            val user = if (authSettings.domain.isBlank()) {
                configuredUser
            } else {
                "${authSettings.domain.trim()};$configuredUser"
            }
            contexts.add(baseContext.withCredentials(NtlmPasswordAuthenticator(user, authSettings.password)))
        }

        contexts.add(baseContext)
        return contexts
    }

    private fun copyDirectoryRecursive(sourceDirectory: SmbFile, targetDirectory: File): Int {
        targetDirectory.mkdirs()
        var copiedFiles = 0

        sourceDirectory.listFiles().orEmpty().forEach { child ->
            if (child.isDirectory) {
                copiedFiles += copyDirectoryRecursive(child, File(targetDirectory, child.name))
            } else {
                copySmbFile(child, File(targetDirectory, child.name))
                copiedFiles += 1
            }
        }

        return copiedFiles
    }

    private fun copySmbFile(source: SmbFile, target: File) {
        target.parentFile?.mkdirs()
        source.inputStream.use { input ->
            FileOutputStream(target).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun Array<out SmbFile>?.orEmpty(): List<SmbFile> = this?.toList() ?: emptyList()
}