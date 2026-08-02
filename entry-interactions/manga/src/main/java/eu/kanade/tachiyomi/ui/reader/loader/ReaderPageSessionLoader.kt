package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.toReaderChapter
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.DownloadProvider
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class ReaderPageSessionLoader(
    private val context: Context,
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadProvider: DownloadProvider = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) {
    suspend fun load(entry: Entry, chapter: EntryChapter): ReaderChapter {
        val readerChapter = ReaderChapter(
            chapter = chapter.toReaderChapter(),
            manga = entry,
        )
        readerChapter.ref()
        try {
            ChapterLoader(
                context = context,
                downloadManager = downloadManager,
                downloadProvider = downloadProvider,
                manga = entry,
                sourceManager = sourceManager,
            ).loadChapter(readerChapter)
            return readerChapter
        } catch (e: Throwable) {
            readerChapter.unref()
            throw e
        }
    }
}
