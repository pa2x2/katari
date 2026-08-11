package eu.kanade.tachiyomi.ui.reader.loader

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryImageSource
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import kotlinx.coroutines.CancellationException
import mihon.core.archive.archiveReader
import mihon.core.archive.epubReader
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.DownloadProvider
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.source.model.UnifiedStubSource
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.MR
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.io.Format

/**
 * Loader used to retrieve the [PageLoader] for a given chapter.
 */
internal class ChapterLoader(
    private val context: Context,
    private val downloadManager: DownloadManager,
    private val downloadProvider: DownloadProvider,
    private val manga: Entry,
    private val sourceManager: SourceManager,
) {

    /**
     * Assigns the chapter's page loader and loads the its pages. Returns immediately if the chapter
     * is already loaded.
     */
    suspend fun loadChapter(chapter: ReaderChapter) {
        if (chapterIsReady(chapter)) {
            return
        }

        if (chapter.state is ReaderChapter.State.Error) {
            chapter.pageLoader?.recycle()
            chapter.pageLoader = null
        }
        chapter.state = ReaderChapter.State.Loading
        withIOContext {
            logcat { "Loading pages for ${chapter.chapter.name}" }
            var pageLoader: PageLoader? = null
            try {
                val resolvedLoader = getPageLoader(chapter)
                pageLoader = resolvedLoader
                chapter.pageLoader = resolvedLoader

                val pages = resolvedLoader.getPages()
                    .onEach { it.chapter = chapter }

                if (pages.isEmpty()) {
                    throw ReaderLoadException(
                        message = context.stringResource(MR.strings.page_list_empty_error),
                        canRetry = !resolvedLoader.isLocal,
                    )
                }

                chapter.state = ReaderChapter.State.Loaded(pages)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    chapter.state = ReaderChapter.State.Error(e)
                    throw e
                }
                val failure = when {
                    e is ReaderLoadException -> e
                    pageLoader?.isLocal == true -> ReaderLoadException(
                        message = e.message ?: "The local chapter content could not be read.",
                        canRetry = false,
                        cause = e,
                    )
                    else -> ReaderLoadException(
                        message = e.message ?: "The chapter pages could not be loaded.",
                        canRetry = true,
                        cause = e,
                    )
                }
                chapter.state = ReaderChapter.State.Error(failure)
                throw failure
            }
        }
    }

    /**
     * Checks [chapter] to be loaded based on present pages and loader in addition to state.
     */
    private fun chapterIsReady(chapter: ReaderChapter): Boolean {
        return chapter.state is ReaderChapter.State.Loaded && chapter.pageLoader != null
    }

    /**
     * Returns the page loader to use for this [chapter].
     */
    private fun getPageLoader(chapter: ReaderChapter): PageLoader {
        val dbChapter = chapter.chapter
        val chapterManga = chapter.manga ?: manga
        val chapterUnifiedSource = sourceManager.getOrStub(chapterManga.source)
        val localSource = chapterUnifiedSource as? LocalSource
        val isDownloaded = downloadManager.isChapterDownloaded(
            dbChapter.name,
            dbChapter.scanlator,
            dbChapter.url,
            chapterManga.title,
            chapterManga.source,
            skipCache = true,
        )
        return when {
            isDownloaded -> DownloadPageLoader(
                chapter,
                chapterManga,
                chapterUnifiedSource,
                downloadManager,
                downloadProvider,
            )
            localSource != null -> {
                try {
                    localSource.getFormat(chapter.chapter.url).let { format ->
                        when (format) {
                            is Format.Directory -> DirectoryPageLoader(format.file)
                            is Format.Archive -> ArchivePageLoader(format.file.archiveReader(context))
                            is Format.Epub -> EpubPageLoader(format.file.epubReader(context))
                        }
                    }
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    throw ReaderLoadException(
                        message = error.message ?: "The local chapter content could not be read.",
                        canRetry = false,
                        cause = error,
                    )
                }
            }
            chapterUnifiedSource is EntryImageSource -> EntryPageLoader(chapter, chapterUnifiedSource)
            chapterUnifiedSource is UnifiedStubSource -> throw ReaderLoadException(
                message = context.stringResource(MR.strings.source_not_installed, chapterUnifiedSource.toString()),
                canRetry = false,
            )
            else -> throw ReaderLoadException(
                message = context.stringResource(MR.strings.loader_not_implemented_error),
                canRetry = false,
            )
        }
    }
}
