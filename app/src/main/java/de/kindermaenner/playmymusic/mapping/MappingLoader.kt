package de.kindermaenner.playmymusic.mapping

import android.content.Context
import de.kindermaenner.playmymusic.offline.OfflineLibraryStorage

class AssetMappingLoader(private val context: Context) {

    private val assetDirectories = listOf("mappings", "local/mappings")
    private val offlineLibraryStorage = OfflineLibraryStorage(context)

    fun loadJsonFiles(mappingFilePostfix: String): List<String> {
        val assetManager = context.assets
        val normalizedPostfix = mappingFilePostfix.trim().removeSuffix(".json")

        val offlineJsonFiles = offlineLibraryStorage.localMappingFiles()
            .filter { matchesFileFilter(it.name, normalizedPostfix) }
            .map { it.readText() }

        val files = assetDirectories.flatMap { directory ->
            assetManager.list(directory)
                ?.filter { fileName ->
                    fileName.endsWith(".json") && matchesFileFilter(fileName, normalizedPostfix)
                }
                ?.map { "$directory/$it" }
                ?: emptyList()
        }

        return files.map { filePath ->
            assetManager.open(filePath)
                .bufferedReader()
                .use { it.readText() }
        } + offlineJsonFiles
    }

    private fun matchesFileFilter(fileName: String, filterValue: String): Boolean {
        if (filterValue.isBlank()) {
            return true
        }

        val nameWithoutJsonSuffix = fileName.removeSuffix(".json")
        return nameWithoutJsonSuffix.endsWith(filterValue) || fileName.startsWith(filterValue)
    }
}
