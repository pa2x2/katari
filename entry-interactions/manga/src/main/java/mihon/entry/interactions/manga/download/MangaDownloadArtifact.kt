package mihon.entry.interactions.manga.download

import com.hippo.unifile.UniFile
import tachiyomi.core.common.storage.extension

private val pageFileName = Regex("^\\d+(?:\\.|__001\\.)")

internal fun UniFile.isValidMangaChapterArtifact(): Boolean = when {
    isFile -> extension.equals("cbz", ignoreCase = true) && length() != 0L
    isDirectory -> listFiles().orEmpty().any { file ->
        file.isFile && pageFileName.containsMatchIn(file.name.orEmpty()) && file.length() != 0L
    }
    else -> false
}

internal fun UniFile.recoverMangaPublicationBackups() {
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
