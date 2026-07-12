package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.DownloadProvider
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.service.SourceManager
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class ReaderPreviewLoader(
    private val context: Context,
    private val downloadManager: DownloadManager = Injekt.get(),
    private val downloadProvider: DownloadProvider = Injekt.get(),
    private val sourceManager: SourceManager = Injekt.get(),
) {

    suspend fun loadChapter(entry: Entry, chapter: ReaderChapter) {
        ChapterLoader(
            context = context,
            downloadManager = downloadManager,
            downloadProvider = downloadProvider,
            manga = entry,
            sourceManager = sourceManager,
        ).loadChapter(chapter)
    }
}
