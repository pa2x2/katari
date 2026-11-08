package eu.kanade.tachiyomi.ui.reader.loader

import app.katari.core.archive.ArchiveOpener
import app.katari.entry.interactions.manga.download.DownloadManager
import app.katari.entry.interactions.manga.download.DownloadProvider
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.toEntryChapter
import tachiyomi.core.common.storage.StorageFile
import tachiyomi.domain.entry.model.Entry

/**
 * Loader used to load a chapter from the downloaded chapters.
 */
internal class DownloadPageLoader(
    private val archives: ArchiveOpener,
    private val chapter: ReaderChapter,
    private val manga: Entry,
    private val source: UnifiedSource,
    private val downloadManager: DownloadManager,
    private val downloadProvider: DownloadProvider,
) : PageLoader() {

    private var archivePageLoader: ArchivePageLoader? = null

    override var isLocal: Boolean = true

    override suspend fun getPages(): List<ReaderPage> {
        val dbChapter = chapter.chapter
        val chapterPath = downloadProvider.findChapterDir(
            dbChapter.name,
            dbChapter.scanlator,
            dbChapter.url,
            manga.downloadFolderTitle,
            source,
        )
        return if (chapterPath?.isFile == true) {
            getPagesFromArchive(chapterPath)
        } else {
            getPagesFromDirectory()
        }
    }

    override fun recycle() {
        super.recycle()
        archivePageLoader?.recycle()
    }

    private suspend fun getPagesFromArchive(file: StorageFile): List<ReaderPage> {
        val loader = ArchivePageLoader(archives.open(file)).also { archivePageLoader = it }
        return loader.getPages()
    }

    private fun getPagesFromDirectory(): List<ReaderPage> {
        val files = downloadManager.pageFiles(source, manga, chapter.chapter.toDomainChapter()!!.toEntryChapter())
        return files.mapIndexed { index, file ->
            ReaderPage(index, stream = file::openInputStream).apply {
                status = Page.State.Ready
            }
        }
    }

    override suspend fun loadPage(page: ReaderPage) {
        archivePageLoader?.loadPage(page)
    }
}
