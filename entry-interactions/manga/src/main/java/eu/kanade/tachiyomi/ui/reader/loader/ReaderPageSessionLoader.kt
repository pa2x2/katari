package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.toReaderChapter
import kotlinx.coroutines.CoroutineScope
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.DownloadProvider
import mihon.entry.interactions.manga.page.MangaPageStore
import mihon.entry.interactions.manga.page.acquisition.MangaPageAcquisitionCoordinator
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

internal class ReaderPageSessionLoader(
    private val downloadManager: DownloadManager,
    private val downloadProvider: DownloadProvider,
    private val sourceManager: SourceManager,
    private val pageStore: MangaPageStore,
    private val pageAcquisition: MangaPageAcquisitionCoordinator,
    private val appScope: CoroutineScope,
) {
    fun chapterLoader(context: Context, manga: Entry): ChapterLoader = ChapterLoader(
        context = context,
        downloadManager = downloadManager,
        downloadProvider = downloadProvider,
        manga = manga,
        sourceManager = sourceManager,
        pageStore = pageStore,
        pageAcquisition = pageAcquisition,
        appScope = appScope,
    )

    suspend fun load(context: Context, entry: Entry, chapter: EntryChapter): ReaderChapter {
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
