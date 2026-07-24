package mihon.entry.interactions

import android.content.Context
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

interface EntryPreviewInteraction {
    fun processor(type: EntryType): EntryPreviewProcessor?
    fun configuration(type: EntryType): EntryPreviewConfigurationProvider?
    suspend fun loadPreview(
        context: Context,
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
        context: Context,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
    ): EntryImmersiveHandle

    fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer

    suspend fun persistProgress(handle: EntryImmersiveHandle, progress: EntryImmersiveProgress)

    fun release(handle: EntryImmersiveHandle)
}
