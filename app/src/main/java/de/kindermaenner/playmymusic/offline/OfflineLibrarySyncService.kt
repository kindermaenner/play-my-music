package de.kindermaenner.playmymusic.offline

import android.content.Context
import com.google.gson.Gson
import de.kindermaenner.playmymusic.data.model.MappingFile
import de.kindermaenner.playmymusic.settings.AppSettings
import de.kindermaenner.playmymusic.settings.SmbAuthSettings
import jcifs.CIFSContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class SyncProgress(
    val current: Int,
    val total: Int,
    val currentItem: String = ""
)

data class SyncResult(
    val message: String,
    val copiedMappings: Int = 0,
    val copiedImages: Int = 0,
    val copiedTracks: Int = 0,
    val totalTracks: Int = 0
)

private data class EditionSyncCount(
    val mappingsCount: Int,
    val imagesCount: Int,
    val tracksCount: Int
)

class OfflineLibrarySyncService(
    private val context: Context,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private val gson = Gson()
    private val storage = OfflineLibraryStorage(context)

    suspend fun syncMetadata(settings: AppSettings): SyncResult {
        return withContext(ioDispatcher) {
            sync(settings = settings, includeTracks = false)
        }
    }

    suspend fun syncTracks(
        settings: AppSettings,
        onProgress: ((SyncProgress) -> Unit)? = null
    ): SyncResult {
        return withContext(ioDispatcher) {
            sync(settings = settings, includeTracks = true, onProgress = onProgress)
        }
    }

    private fun sync(
        settings: AppSettings,
        includeTracks: Boolean,
        onProgress: ((SyncProgress) -> Unit)? = null
    ): SyncResult {
        val root = resolveBaseDirectory(settings)
        val editionDirectories = root.listFiles().orEmpty().filter { it.isDirectory }

        val tracksFolderName = settings.trackFolderName.ifBlank { "tracks" }
        val totalTracks = if (includeTracks) countTotalTracks(editionDirectories, tracksFolderName) else 0

        var copiedMappings = 0
        var copiedImages = 0
        var copiedTracks = 0
        var currentTrackIndex = 0

        editionDirectories.forEach { editionDirectory ->
            val editionCount = syncSingleEdition(
                editionDirectory = editionDirectory,
                tracksFolderName = tracksFolderName,
                includeTracks = includeTracks,
                onTrackCopied = { file ->
                    currentTrackIndex++
                    onProgress?.invoke(
                        SyncProgress(
                            current = currentTrackIndex,
                            total = totalTracks,
                            currentItem = file.name.trimEnd('/', '\\')
                        )
                    )
                }
            )
            copiedMappings += editionCount.mappingsCount
            copiedImages += editionCount.imagesCount
            copiedTracks += editionCount.tracksCount
        }

        val message = if (includeTracks) {
            "Synced $copiedMappings mapping files, $copiedImages images, and $copiedTracks tracks (total tracks: $totalTracks)."
        } else {
            "Synced $copiedMappings mapping files and $copiedImages images."
        }

        return SyncResult(
            message = message,
            copiedMappings = copiedMappings,
            copiedImages = copiedImages,
            copiedTracks = copiedTracks,
            totalTracks = totalTracks
        )
    }

    private fun countTotalTracks(editionDirectories: List<SmbFile>, tracksFolderName: String): Int {
        return editionDirectories.sumOf { edDir ->
            val remoteTracks = SmbFile(edDir, "$tracksFolderName/")
            if (remoteTracks.exists() && remoteTracks.isDirectory) {
                remoteTracks.listFiles().orEmpty().count { !it.isDirectory }
            } else {
                0
            }
        }
    }

    private fun syncSingleEdition(
        editionDirectory: SmbFile,
        tracksFolderName: String,
        includeTracks: Boolean,
        onTrackCopied: (SmbFile) -> Unit
    ): EditionSyncCount {
        val rawName = editionDirectory.name.trim().trimEnd('/', '\\')
        val initialTargetDir = storage.editionDir(rawName).apply { mkdirs() }
        val mappingFile = editionDirectory.listFiles().orEmpty()
            .firstOrNull { it.isFile && it.name.endsWith(".json", ignoreCase = true) }
            ?: return EditionSyncCount(0, 0, 0)

        val localMappingFile = File(initialTargetDir, mappingFile.name)
        copySmbFileAtomic(mappingFile, localMappingFile)

        val mapping = runCatching {
            gson.fromJson(localMappingFile.readText(), MappingFile::class.java)
        }.getOrNull()

        val targetEditionDirectory = resolveTargetEditionDir(initialTargetDir, localMappingFile, mapping)
        val imagesCount = syncEditionImage(editionDirectory, targetEditionDirectory, mapping)
        val tracksCount = if (includeTracks) {
            syncEditionTracks(editionDirectory, targetEditionDirectory, tracksFolderName, onTrackCopied)
        } else {
            0
        }

        return EditionSyncCount(
            mappingsCount = 1,
            imagesCount = imagesCount,
            tracksCount = tracksCount
        )
    }

    private fun resolveTargetEditionDir(
        initialTargetDir: File,
        localMappingFile: File,
        mapping: MappingFile?
    ): File {
        val editionFromMeta = mapping?.meta?.edition?.trim().orEmpty()
        if (editionFromMeta.isBlank()) return initialTargetDir

        val metaDir = storage.editionDir(editionFromMeta).apply { mkdirs() }
        if (metaDir.absolutePath != initialTargetDir.absolutePath) {
            localMappingFile.copyTo(File(metaDir, localMappingFile.name), overwrite = true)
        }
        return metaDir
    }

    private fun syncEditionImage(
        editionDirectory: SmbFile,
        targetEditionDirectory: File,
        mapping: MappingFile?
    ): Int {
        val imageName = mapping?.meta?.image?.trim().orEmpty()
        if (imageName.isBlank()) return 0

        val imageSource = SmbFile(editionDirectory, imageName)
        return if (imageSource.exists()) {
            val copied = copySmbFileAtomic(imageSource, File(targetEditionDirectory, imageName))
            if (copied) 1 else 0
        } else {
            0
        }
    }

    private fun syncEditionTracks(
        editionDirectory: SmbFile,
        targetEditionDirectory: File,
        tracksFolderName: String,
        onTrackCopied: (SmbFile) -> Unit
    ): Int {
        val remoteTracksDirectory = SmbFile(editionDirectory, "$tracksFolderName/")
        if (!remoteTracksDirectory.exists() || !remoteTracksDirectory.isDirectory) {
            return 0
        }

        val targetTracksDir = File(targetEditionDirectory, tracksFolderName)
        return copyDirectoryRecursive(
            sourceDirectory = remoteTracksDirectory,
            targetDirectory = targetTracksDir,
            onFileCopied = onTrackCopied
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

    internal fun buildBaseUri(settings: AppSettings): String {
        val host = settings.playback.nasAddress.trim().trim('\\', '/')
        val baseFolder = settings.playback.nasBaseFolder.trim().replace('\\', '/')
        return if (baseFolder.isBlank()) {
            "smb://$host/"
        } else {
            "smb://$host/${baseFolder.trim('/')}/"
        }
    }

    internal fun buildCandidateContexts(authSettings: SmbAuthSettings): List<CIFSContext> {
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

    private fun copyDirectoryRecursive(
        sourceDirectory: SmbFile,
        targetDirectory: File,
        onFileCopied: ((SmbFile) -> Unit)? = null
    ): Int {
        targetDirectory.mkdirs()
        var copiedFiles = 0

        sourceDirectory.listFiles().orEmpty().forEach { child ->
            val cleanChildName = child.name.trim().trimEnd('/', '\\')
            if (child.isDirectory) {
                copiedFiles += copyDirectoryRecursive(
                    sourceDirectory = child,
                    targetDirectory = File(targetDirectory, cleanChildName),
                    onFileCopied = onFileCopied
                )
            } else {
                val targetFile = File(targetDirectory, cleanChildName)
                val wasCopied = copySmbFileAtomic(child, targetFile)
                if (wasCopied) {
                    copiedFiles += 1
                }
                onFileCopied?.invoke(child)
            }
        }

        return copiedFiles
    }

    internal fun copySmbFileAtomic(source: SmbFile, target: File): Boolean {
        target.parentFile?.mkdirs()
        if (shouldSkipCopy(source, target)) {
            return false
        }

        val tempFile = File(target.parentFile, "${target.name}.tmp")
        source.inputStream.use { input ->
            FileOutputStream(tempFile).use { output ->
                input.copyTo(output)
            }
        }

        if (tempFile.exists()) {
            if (target.exists() && !target.delete()) {
                logWarning("Failed to delete existing target file before replacement: ${target.absolutePath}")
            }
            if (!tempFile.renameTo(target)) {
                tempFile.copyTo(target, overwrite = true)
                if (!tempFile.delete()) {
                    logWarning("Failed to delete temporary file: ${tempFile.absolutePath}")
                }
            }
        }

        val remoteTime = source.lastModified()
        if (remoteTime > 0) {
            val updated = target.setLastModified(remoteTime)
            if (!updated) {
                logWarning("Failed to set lastModified timestamp on: ${target.absolutePath}")
            }
        }
        return true
    }

    internal fun shouldSkipCopy(source: SmbFile, target: File): Boolean {
        if (!target.exists() || target.length() != source.length()) {
            return false
        }
        val remoteTime = source.lastModified()
        val localTime = target.lastModified()
        return remoteTime <= 0 || Math.abs(remoteTime - localTime) < 2000
    }

    private fun logWarning(message: String) {
        System.err.println("OfflineLibrarySyncService: $message")
    }

    private fun Array<out SmbFile>?.orEmpty(): List<SmbFile> = this?.toList() ?: emptyList()
}
