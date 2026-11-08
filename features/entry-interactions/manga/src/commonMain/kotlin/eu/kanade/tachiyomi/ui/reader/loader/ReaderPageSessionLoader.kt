package eu.kanade.tachiyomi.ui.reader.loader

import app.katari.core.archive.ArchiveOpener
import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.manga.download.DownloadManager
import app.katari.entry.interactions.manga.download.DownloadProvider
import app.katari.entry.interactions.manga.page.MangaPageStore
import app.katari.entry.interactions.manga.page.acquisition.MangaPageAcquisitionCoordinator
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.toReaderChapter
import kotlinx.coroutines.CoroutineScope
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

internal class ReaderPageSessionLoader(
    private val archives: ArchiveOpener,
    private val downloadManager: DownloadManager,
    private val downloadProvider: DownloadProvider,
    private val sourceManager: SourceManager,
    private val pageStore: MangaPageStore,
    private val pageAcquisition: MangaPageAcquisitionCoordinator,
    private val appScope: CoroutineScope,
) {
    fun chapterLoader(context: PlatformContext, manga: Entry): ChapterLoader = ChapterLoader(
        context = context,
        archives = archives,
        downloadManager = downloadManager,
        downloadProvider = downloadProvider,
        manga = manga,
        sourceManager = sourceManager,
        pageStore = pageStore,
        pageAcquisition = pageAcquisition,
        appScope = appScope,
    )

    suspend fun load(context: PlatformContext, entry: Entry, chapter: EntryChapter): ReaderChapter {
        val readerChapter = ReaderChapter(
            chapter = chapter.toReaderChapter(),
            manga = entry,
        )
        readerChapter.ref()
        try {
            chapterLoader(context, entry).loadChapter(readerChapter)
            return readerChapter
        } catch (e: Throwable) {
            readerChapter.unref()
            throw e
        }
    }
}
