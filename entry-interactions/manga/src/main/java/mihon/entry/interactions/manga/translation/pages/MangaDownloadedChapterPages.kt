package mihon.entry.interactions.manga.translation.pages

import android.content.Context
import com.hippo.unifile.UniFile
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.util.lang.compareToCaseInsensitiveNaturalOrder
import mihon.core.archive.archiveReader
import mihon.entry.interactions.manga.download.DownloadProvider
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.ImageUtil
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** One raw page file of a downloaded chapter; [read] returns its bytes as stored. */
internal class MangaDownloadedPage(
    val fileName: String,
    val read: () -> ByteArray,
)

/** Reads the raw page files of downloaded chapters, stored as a folder of images or as an archive. */
internal class MangaDownloadedChapterPages(
    private val context: Context,
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
            provider.findChapterDir(chapter.name, chapter.scanlator, chapter.url, entry.title, source)
        } ?: return null
        return if (artifact.isFile) archivePages(artifact, block) else block(folderPages(artifact))
    }

    private suspend fun <T> archivePages(archive: UniFile, block: suspend (List<MangaDownloadedPage>) -> T): T =
        withIOContext { archive.archiveReader(context) }.use { reader ->
            val names = withIOContext {
                reader.useEntries { entries ->
                    entries
                        .filter { it.isFile && ImageUtil.isImage(it.name) { reader.getInputStream(it.name)!! } }
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

    private suspend fun folderPages(folder: UniFile): List<MangaDownloadedPage> = withIOContext {
        folder.listFiles().orEmpty()
            .filter { it.isFile && ImageUtil.isImage(it.name) { it.openInputStream() } }
            .sortedBy { it.name }
            .map { file -> MangaDownloadedPage(file.name.orEmpty()) { file.openInputStream().use { it.readBytes() } } }
    }
}
