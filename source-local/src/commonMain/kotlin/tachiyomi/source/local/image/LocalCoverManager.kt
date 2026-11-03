package tachiyomi.source.local.image

import tachiyomi.core.common.storage.StorageFile
import tachiyomi.core.common.storage.nameWithoutExtension
import tachiyomi.core.common.util.system.isImageFile
import tachiyomi.source.local.LocalEntryMetadata
import tachiyomi.source.local.io.LocalSourceFileSystem
import java.io.InputStream

private const val DEFAULT_COVER_NAME = "cover.jpg"

class LocalCoverManager(
    private val fileSystem: LocalSourceFileSystem,
    private val mediaScanner: MediaScanner,
) {

    fun find(mangaUrl: String): StorageFile? {
        return fileSystem.getFilesInMangaDirectory(mangaUrl)
            // Get all file whose names start with "cover"
            .filter { it.isFile && it.nameWithoutExtension.equals("cover", ignoreCase = true) }
            // Get the first actual image
            .firstOrNull { isImageFile(it.name) { it.openInputStream() } }
    }

    internal fun update(
        entry: LocalEntryMetadata,
        inputStream: InputStream,
    ): StorageFile? {
        val directory = fileSystem.getMangaDirectory(entry.url)
        if (directory == null) {
            inputStream.close()
            return null
        }

        val targetFile = find(entry.url) ?: directory.createFile(DEFAULT_COVER_NAME)!!

        inputStream.use { input ->
            targetFile.openOutputStream().use { output ->
                input.copyTo(output)
            }
        }

        mediaScanner.exclude(directory)

        entry.thumbnailUrl = targetFile.uri
        return targetFile
    }

    fun update(mangaUrl: String, inputStream: InputStream): StorageFile? {
        return update(LocalEntryMetadata(url = mangaUrl, title = mangaUrl), inputStream)
    }
}
