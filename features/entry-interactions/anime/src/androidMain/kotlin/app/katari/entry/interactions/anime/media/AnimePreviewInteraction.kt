package app.katari.entry.interactions.anime.media

import android.content.Context
import app.katari.entry.interactions.anime.runtime.requireAnime
import app.katari.entry.interactions.media.EntryPreviewConfig
import app.katari.entry.interactions.media.EntryPreviewHandle
import app.katari.entry.interactions.media.EntryPreviewPage
import app.katari.entry.interactions.media.EntryPreviewPageStatus
import app.katari.entry.interactions.media.EntryPreviewSettings
import app.katari.entry.interactions.media.EntryPreviewSourceRequirement
import app.katari.entry.interactions.runtime.EntryPreviewConfigurationProvider
import app.katari.entry.interactions.runtime.EntryPreviewLoadMode
import app.katari.entry.interactions.runtime.EntryPreviewProcessor
import app.katari.entry.interactions.settings.EntryInteractionPreferences
import eu.kanade.tachiyomi.source.entry.EntryPreviewSource
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import tachiyomi.domain.entry.adapter.toSEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

internal class AnimePreviewInteraction(
    private val entryInteractionPreferences: EntryInteractionPreferences,
) : EntryPreviewProcessor, EntryPreviewConfigurationProvider {
    override val loadMode = EntryPreviewLoadMode.ENTRY
    override val settings = EntryPreviewSettings(
        type = EntryType.ANIME,
        enabled = entryInteractionPreferences.enableAnimePreview,
        pageCount = entryInteractionPreferences.animePreviewPageCount,
        size = entryInteractionPreferences.animePreviewSize,
    )
    override val sourceRequirement = EntryPreviewSourceRequirement.PREVIEW_CAPABILITY

    override fun config(): EntryPreviewConfig {
        return EntryPreviewConfig(
            enabled = entryInteractionPreferences.enableAnimePreview.get(),
            pageCount = entryInteractionPreferences.animePreviewPageCount.get(),
            size = entryInteractionPreferences.animePreviewSize.get(),
        )
    }

    override fun configChanges(): Flow<EntryPreviewConfig> {
        return combine(
            entryInteractionPreferences.enableAnimePreview.changes(),
            entryInteractionPreferences.animePreviewPageCount.changes(),
            entryInteractionPreferences.animePreviewSize.changes(),
        ) { enabled, pageCount, size ->
            EntryPreviewConfig(
                enabled = enabled,
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
