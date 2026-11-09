package app.katari.entry.interactions.runtime

import app.katari.core.common.platform.PlatformFacility
import app.katari.core.platform.PlatformContext
import app.katari.entry.interactions.child.EntryChildListDisplay
import app.katari.entry.interactions.child.EntryChildListRequest
import app.katari.entry.interactions.child.EntryChildProgressLabel
import app.katari.entry.interactions.child.EntryChildProgressRequest
import app.katari.entry.interactions.media.EntryImmersiveHandle
import app.katari.entry.interactions.media.EntryImmersiveRenderer
import app.katari.entry.interactions.media.EntryPreviewConfig
import app.katari.entry.interactions.media.EntryPreviewHandle
import app.katari.entry.interactions.media.EntryPreviewSettings
import app.katari.entry.interactions.media.EntryPreviewSourceRequirement
import app.katari.entry.interactions.presentation.EntryTypePresentation
import eu.kanade.tachiyomi.source.entry.UnifiedSource
import kotlinx.coroutines.flow.Flow
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

enum class EntryImmersiveLoadMode {
    ENTRY,
    FIRST_READING_CHILD,
}

@PlatformFacility
interface EntryImmersiveProcessor {
    val loadMode: EntryImmersiveLoadMode

    /** How many titles after the shown one the feed loads and opens, so moving on starts them at once. */
    val preloadAhead: Int

    suspend fun load(
        context: PlatformContext,
        entry: Entry,
        chapter: EntryChapter?,
        source: UnifiedSource,
    ): EntryImmersiveHandle

    fun renderer(handle: EntryImmersiveHandle): EntryImmersiveRenderer

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

interface EntryScanlatorFilterProcessor {
    fun scanlatorFor(entry: Entry, chapter: EntryChapter): String?
    fun normalizeScanlator(entry: Entry, scanlator: String): String?
}

interface EntryOutsideReleasePeriodFilterProvider

enum class EntryPreviewLoadMode {
    ENTRY,
    FIRST_READING_CHILD,
}

@PlatformFacility
interface EntryPreviewProcessor {
    val loadMode: EntryPreviewLoadMode
    val sourceRequirement: EntryPreviewSourceRequirement
        get() = EntryPreviewSourceRequirement.NONE

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

@PlatformFacility
interface EntryPreviewConfigurationProvider {
    val settings: EntryPreviewSettings
    fun config(): EntryPreviewConfig
    fun configChanges(): Flow<EntryPreviewConfig>
}

interface EntryTypePresentationProvider {
    val presentation: EntryTypePresentation
}
