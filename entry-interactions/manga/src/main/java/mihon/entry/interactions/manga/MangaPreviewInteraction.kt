package mihon.entry.interactions.manga

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.loader.ReaderPreviewLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.toReaderChapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import mihon.entry.interactions.EntryPreviewConfig
import mihon.entry.interactions.EntryPreviewHandle
import mihon.entry.interactions.EntryPreviewPage
import mihon.entry.interactions.EntryPreviewPageStatus
import mihon.entry.interactions.EntryPreviewProcessor
import mihon.entry.interactions.settings.EntryInteractionPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class MangaPreviewInteraction(
    private val entryInteractionPreferences: EntryInteractionPreferences,
) : EntryPreviewProcessor {
    override val type: EntryType = EntryType.MANGA

    override fun isSupported(entry: Entry): Boolean {
        return entry.type == EntryType.MANGA
    }

    override fun requiresChapter(entry: Entry): Boolean = true

    override fun config(entry: Entry): EntryPreviewConfig {
        return EntryPreviewConfig(
            enabled = isSupported(entry) && entryInteractionPreferences.enableMangaPreview.get(),
            pageCount = entryInteractionPreferences.mangaPreviewPageCount.get(),
            size = entryInteractionPreferences.mangaPreviewSize.get(),
        )
    }

    override fun configChanges(entry: Entry): Flow<EntryPreviewConfig> {
        return combine(
            entryInteractionPreferences.enableMangaPreview.changes(),
            entryInteractionPreferences.mangaPreviewPageCount.changes(),
            entryInteractionPreferences.mangaPreviewSize.changes(),
        ) { enabled, pageCount, size ->
            EntryPreviewConfig(
                enabled = isSupported(entry) && enabled,
                pageCount = pageCount,
                size = size,
            )
        }
            .distinctUntilChanged()
    }

    override suspend fun loadPreview(
        context: Context,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
        pageCount: Int,
    ): EntryPreviewHandle {
        entry.requireManga()
        requireNotNull(chapter) { "Manga previews require a chapter" }
        val readerChapter = ReaderChapter(
            chapter = chapter.toReaderChapter(),
            manga = entry,
        )
        readerChapter.ref()
        ReaderPreviewLoader(context).loadChapter(entry, readerChapter)

        return EntryPreviewHandle(
            entryType = EntryType.MANGA,
            chapterId = chapter.id,
            pages = readerChapter.pages
                .orEmpty()
                .take(pageCount)
                .map { page ->
                    EntryPreviewPage(
                        index = page.index,
                        status = page.statusFlow.mapState(Page.State::toEntryPreviewPageStatus),
                        progress = page.progressFlow,
                        imageModel = page,
                    )
                },
            delegate = readerChapter,
        )
    }

    override suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int) {
        val chapter = handle.delegate as? ReaderChapter ?: return
        val page = chapter.pages?.getOrNull(pageIndex) ?: return
        chapter.pageLoader?.loadPage(page)
    }

    override fun release(handle: EntryPreviewHandle) {
        (handle.delegate as? ReaderChapter)?.unref()
    }
}

private fun Page.State.toEntryPreviewPageStatus(): EntryPreviewPageStatus {
    return when (this) {
        Page.State.Queue -> EntryPreviewPageStatus.Queued
        Page.State.LoadPage -> EntryPreviewPageStatus.LoadingPage
        Page.State.DownloadImage -> EntryPreviewPageStatus.DownloadingImage
        Page.State.Ready -> EntryPreviewPageStatus.Ready
        is Page.State.Error -> EntryPreviewPageStatus.Error(error)
    }
}

@OptIn(
    ExperimentalCoroutinesApi::class,
    ExperimentalForInheritanceCoroutinesApi::class,
    InternalCoroutinesApi::class,
)
private class MappedStateFlow<T, R>(
    private val upstream: StateFlow<T>,
    private val transform: (T) -> R,
) : StateFlow<R> {
    override val replayCache: List<R>
        get() = listOf(value)

    override val value: R
        get() = transform(upstream.value)

    override suspend fun collect(collector: FlowCollector<R>): Nothing {
        upstream.collect { collector.emit(transform(it)) }
        error("StateFlow collection completed unexpectedly")
    }
}

private fun <T, R> StateFlow<T>.mapState(transform: (T) -> R): StateFlow<R> {
    return MappedStateFlow(this, transform)
}
