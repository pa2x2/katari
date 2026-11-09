package app.katari.entry.interactions.manga.translation.pages

import app.katari.core.archive.ArchiveOpener
import app.katari.entry.interactions.manga.download.DownloadProvider
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.isImageFile
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** One raw page file of a downloaded chapter; [read] returns its bytes as stored. */
internal class MangaDownloadedPage(
    val fileName: String,
    val read: () -> ByteArray,
)

/** Reads the raw page files of downloaded chapters, stored as a folder of images or as an archive. */
internal class MangaDownloadedChapterPages(
    private val archives: ArchiveOpener,
    private val provider: DownloadProvider,
) {
    /**
     * Passes the chapter's pages, in the order the reader shows them, to [block] while they can be read, or returns
     * `null` when the chapter is not downloaded.
     */
    suspend fun <T> use(
        chapter: EntryChapter,
        entry: Entry,
        source: UnifiedSource,
        block: suspend (List<MangaDownloadedPage>) -> T,
    ): T? {
        val artifact = withIOContext {
            provider.findChapterDir(chapter.name, chapter.scanlator, chapter.url, entry.downloadFolderTitle, source)
        } ?: return null
        return if (artifact.isFile) archivePages(artifact, block) else block(folderPages(artifact))
    }

    private suspend fun <T> archivePages(archive: StorageFile, block: suspend (List<MangaDownloadedPage>) -> T): T =
        withIOContext { archives.open(archive) }.use { reader ->
            val names = withIOContext {
                reader.useEntries { entries ->
                    entries
                        .filter { it.isFile && isImageFile(it.name) { reader.getInputStream(it.name)!! } }
                        .map { it.name }
                        .sortedWith { a, b -> a.compareToCaseInsensitiveNaturalOrder(b) }
                        .toList()
                }
            }
            block(
                names.map { name ->
                    MangaDownloadedPage(name) { checkNotNull(reader.getInputStream(name)).use { it.readBytes() } }
                },
            )
        }

    private suspend fun folderPages(folder: StorageFile): List<MangaDownloadedPage> = withIOContext {
        folder.listFiles().orEmpty()
            .filter { it.isFile && isImageFile(it.name) { it.openInputStream() } }
            .sortedBy { it.name }
            .map { file -> MangaDownloadedPage(file.name.orEmpty()) { file.openInputStream().use { it.readBytes() } } }
    }
}
