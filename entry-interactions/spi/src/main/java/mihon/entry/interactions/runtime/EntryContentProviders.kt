package mihon.entry.interactions.runtime

import android.content.Context
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import mihon.entry.interactions.child.EntryChildListDisplay
import mihon.entry.interactions.child.EntryChildListRequest
import mihon.entry.interactions.child.EntryChildProgressLabel
import mihon.entry.interactions.child.EntryChildProgressRequest
import mihon.entry.interactions.media.EntryImmersiveHandle
import mihon.entry.interactions.media.EntryImmersiveProgress
import mihon.entry.interactions.media.EntryImmersiveRenderer
import mihon.entry.interactions.media.EntryPreviewConfig
import mihon.entry.interactions.media.EntryPreviewHandle
import mihon.entry.interactions.media.EntryPreviewSettings
import mihon.entry.interactions.media.EntryPreviewSourceRequirement
import mihon.entry.interactions.presentation.EntryTypePresentation
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

enum class EntryImmersiveLoadMode {
    ENTRY,
    FIRST_READING_CHILD,
}

interface EntryImmersiveProcessor {
    val loadMode: EntryImmersiveLoadMode
    val preloadRadius: Int

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

interface EntryChildListProcessor {
    fun sortedForReading(entry: Entry, chapters: List<EntryChapter>, memberIds: List<Long>): List<EntryChapter>
    fun sortedForDisplay(entry: Entry, chapters: List<EntryChapter>, memberIds: List<Long>): List<EntryChapter>
}

interface EntryChildProgressProcessor {
    fun progressLabels(request: EntryChildProgressRequest): Flow<Map<Long, EntryChildProgressLabel>>
}

/** Owns missing-child gap rows and counts when that media has meaningful sequential numbering. */
interface EntryMissingChildGapProcessor {
    fun buildDisplayList(request: EntryChildListRequest): EntryChildListDisplay
}

interface EntryChildGroupFilterProcessor {
    fun groupFor(entry: Entry, chapter: EntryChapter): String?
    fun normalizeGroup(entry: Entry, group: String): String?
}

interface EntryOutsideReleasePeriodFilterProvider

enum class EntryPreviewLoadMode {
    ENTRY,
    FIRST_READING_CHILD,
}

interface EntryPreviewProcessor {
    val loadMode: EntryPreviewLoadMode
    val sourceRequirement: EntryPreviewSourceRequirement
        get() = EntryPreviewSourceRequirement.NONE

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

interface EntryPreviewConfigurationProvider {
    val settings: EntryPreviewSettings
    fun config(): EntryPreviewConfig
    fun configChanges(): Flow<EntryPreviewConfig>
}

interface EntryTypePresentationProvider {
    val presentation: EntryTypePresentation
}
