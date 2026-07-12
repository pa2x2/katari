package mihon.entry.interactions.anime

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryPreviewSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import mihon.entry.interactions.EntryPreviewConfig
import mihon.entry.interactions.EntryPreviewHandle
import mihon.entry.interactions.EntryPreviewPage
import mihon.entry.interactions.EntryPreviewPageStatus
import mihon.entry.interactions.EntryPreviewProcessor
import mihon.entry.interactions.settings.EntryInteractionPreferences
import tachiyomi.domain.entry.adapter.toSEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.source.service.SourceManager

internal class AnimePreviewInteraction(
    private val entryInteractionPreferences: EntryInteractionPreferences,
    private val sourceManager: SourceManager,
) : EntryPreviewProcessor {
    override val type: EntryType = EntryType.ANIME

    override fun isSupported(entry: Entry): Boolean {
        return entry.type == EntryType.ANIME && sourceManager.get(entry.source) is EntryPreviewSource
    }

    override fun requiresChapter(entry: Entry): Boolean = false

    override fun config(entry: Entry): EntryPreviewConfig {
        return EntryPreviewConfig(
            enabled = isSupported(entry) && entryInteractionPreferences.enableAnimePreview.get(),
            pageCount = entryInteractionPreferences.animePreviewPageCount.get(),
            size = entryInteractionPreferences.animePreviewSize.get(),
        )
    }

    override fun configChanges(entry: Entry): Flow<EntryPreviewConfig> {
        return combine(
            entryInteractionPreferences.enableAnimePreview.changes(),
            entryInteractionPreferences.animePreviewPageCount.changes(),
            entryInteractionPreferences.animePreviewSize.changes(),
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
        entry.requireAnime()
        val previewSource = source as? EntryPreviewSource
            ?: error("Source ${source.name} does not support entry previews")
        val pages = previewSource.getEntryPreview(entry.toSEntry())
            .take(pageCount)
            .map { preview ->
                EntryPreviewPage(
                    index = preview.index,
                    status = MutableStateFlow<EntryPreviewPageStatus>(EntryPreviewPageStatus.Ready),
                    progress = MutableStateFlow(100),
                    imageModel = preview.imageUrl,
                    canOpen = false,
                )
            }
        return EntryPreviewHandle(
            entryType = EntryType.ANIME,
            chapterId = null,
            pages = pages,
        )
    }

    override suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int) = Unit

    override fun release(handle: EntryPreviewHandle) = Unit
}
