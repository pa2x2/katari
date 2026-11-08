package mihon.entry.interactions.manga.media

import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.loader.ReaderPageSessionLoader
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ExperimentalForInheritanceCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import mihon.core.platform.PlatformContext
import mihon.entry.interactions.manga.runtime.requireManga
import mihon.entry.interactions.media.EntryPreviewConfig
import mihon.entry.interactions.media.EntryPreviewHandle
import mihon.entry.interactions.media.EntryPreviewPage
import mihon.entry.interactions.media.EntryPreviewPageStatus
import mihon.entry.interactions.media.EntryPreviewSettings
import mihon.entry.interactions.runtime.EntryPreviewLoadMode
import mihon.entry.interactions.settings.EntryInteractionPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Previews of a manga chapter's first pages, alike on every platform: the chapter to read, loaded as the reader loads
 * it, its first pages shown as they load, and whether previews are on and how many pages they show. The preview
 * processor and its configuration are platform facilities, so each platform answers them with its own
 * `MangaPreviewProcessor` over these.
 */
internal class MangaPreviews(
    private val entryInteractionPreferences: EntryInteractionPreferences,
    private val pageSessionLoader: Lazy<ReaderPageSessionLoader>,
) {
    val loadMode = EntryPreviewLoadMode.FIRST_READING_CHILD
    val settings = EntryPreviewSettings(
        type = EntryType.MANGA,
        enabled = entryInteractionPreferences.enableMangaPreview,
        pageCount = entryInteractionPreferences.mangaPreviewPageCount,
        size = entryInteractionPreferences.mangaPreviewSize,
    )

    fun config(): EntryPreviewConfig {
        return EntryPreviewConfig(
            enabled = entryInteractionPreferences.enableMangaPreview.get(),
            pageCount = entryInteractionPreferences.mangaPreviewPageCount.get(),
            size = entryInteractionPreferences.mangaPreviewSize.get(),
        )
    }

    fun configChanges(): Flow<EntryPreviewConfig> {
        return combine(
            entryInteractionPreferences.enableMangaPreview.changes(),
            entryInteractionPreferences.mangaPreviewPageCount.changes(),
            entryInteractionPreferences.mangaPreviewSize.changes(),
        ) { enabled, pageCount, size ->
            EntryPreviewConfig(
                enabled = enabled,
                pageCount = pageCount,
                size = size,
            )
        }
            .distinctUntilChanged()
    }

    suspend fun loadPreview(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
        pageCount: Int,
    ): EntryPreviewHandle {
        entry.requireManga()
        requireNotNull(chapter) { "Manga previews require a chapter" }
        val readerChapter = pageSessionLoader.value.load(context, entry, chapter)

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
                        progressiveImageState = page.progressiveImageState,
                        onFinalImageReady = page::releaseProgressivePreviewAfterFinalImageReady,
                    )
                },
            delegate = readerChapter,
        )
    }

    suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int) {
        val chapter = handle.delegate as? ReaderChapter ?: return
        val page = chapter.pages?.getOrNull(pageIndex) ?: return
        chapter.pageLoader?.run {
            selectPage(page)
            loadPage(page)
        }
    }

    fun release(handle: EntryPreviewHandle) {
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
