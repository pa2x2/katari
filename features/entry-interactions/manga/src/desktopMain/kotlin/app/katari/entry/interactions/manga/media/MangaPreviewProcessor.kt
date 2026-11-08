package app.katari.entry.interactions.manga.media

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.media.EntryPreviewHandle
import app.katari.entry.interactions.runtime.EntryPreviewConfigurationProvider
import app.katari.entry.interactions.runtime.EntryPreviewProcessor
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** The desktop's previews of a manga chapter's first pages: the shared [MangaPreviews]. */
internal class MangaPreviewProcessor(
    private val previews: MangaPreviews,
) : EntryPreviewProcessor, EntryPreviewConfigurationProvider {
    override val loadMode get() = previews.loadMode
    override val settings get() = previews.settings

    override fun config() = previews.config()

    override fun configChanges() = previews.configChanges()

    override suspend fun loadPreview(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
        pageCount: Int,
    ): EntryPreviewHandle = previews.loadPreview(context, entry, chapter, source, pageCount)

    override suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int) = previews.loadPage(handle, pageIndex)

    override fun release(handle: EntryPreviewHandle) = previews.release(handle)
}
