package app.katari.entry.interactions.media

import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.runtime.EntryImmersiveProcessor
import app.katari.entry.interactions.runtime.EntryPreviewConfigurationProvider
import app.katari.entry.interactions.runtime.EntryPreviewProcessor
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryPreviewInteraction {
    fun processor(type: EntryType): EntryPreviewProcessor?
    fun configuration(type: EntryType): EntryPreviewConfigurationProvider?
    suspend fun loadPreview(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
        pageCount: Int,
    ): EntryPreviewHandle
    suspend fun loadPage(handle: EntryPreviewHandle, pageIndex: Int)
    fun release(handle: EntryPreviewHandle)
}

interface EntryImmersiveInteraction {
    fun processor(type: EntryType): EntryImmersiveProcessor?

    suspend fun load(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
    ): EntryImmersiveHandle

    fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer

    fun release(handle: EntryImmersiveHandle)
}
