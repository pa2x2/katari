package app.katari.entry.interactions.manga.download

import tachiyomi.core.common.storage.StorageFile
import tachiyomi.core.common.storage.extension
import tachiyomi.core.common.storage.nameWithoutExtension

private val pageFileName = Regex("^\\d+(?:\\.|__001\\.)")

internal fun StorageFile.isValidMangaChapterArtifact(): Boolean = when {
    isFile -> extension.equals("cbz", ignoreCase = true) && length() != 0L
    isDirectory -> listFiles().orEmpty().any { file ->
        file.isFile && pageFileName.containsMatchIn(file.name.orEmpty()) && file.length() != 0L
    }
    else -> false
}

/** The name the download cache indexes this chapter artifact under, that of its folder or of its archive. */
internal val StorageFile.chapterDirName: String?
    get() = if (isFile) nameWithoutExtension else name

internal fun StorageFile.recoverMangaPublicationBackups() {
    listFiles().orEmpty()
        .filter { it.name?.endsWith(Downloader.PUBLISH_BACKUP_SUFFIX) == true }
        .forEach { backup ->
            val finalName = checkNotNull(backup.name).removeSuffix(Downloader.PUBLISH_BACKUP_SUFFIX)
            val published = findFile(finalName)
            if (published?.isValidMangaChapterArtifact() == true) {
                backup.delete()
            } else {
                published?.delete()
                backup.renameTo(finalName)
            }
        }
}
