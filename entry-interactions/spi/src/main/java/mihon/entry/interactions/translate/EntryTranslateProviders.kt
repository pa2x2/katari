package mihon.entry.interactions.translate

import kotlinx.coroutines.flow.Flow
import mihon.entry.interactions.runtime.EntryInteractionProvider
import mihon.entry.interactions.runtime.entryInteractionCapability
import mihon.feature.graph.CapabilityId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Makes and keeps stored translations of one entry type's downloaded chapters. */
interface EntryTranslateProcessor : EntryInteractionProvider {
    /** Emits when stored translations were written or deleted. */
    val changes: Flow<Unit>

    /** Those of [chapters] that have a stored translation. */
    suspend fun translatedChapters(entry: Entry, chapters: List<EntryChapter>): Set<Long>

    /**
     * Translates the downloaded [chapter] with [setup] and stores the result, reporting progress as it goes. Nothing is
     * stored unless it succeeds; cancellation stores nothing either.
     */
    suspend fun translate(
        entry: Entry,
        chapter: EntryChapter,
        setup: EntryTranslateSetup,
        onProgress: (EntryTranslateProgress) -> Unit,
    ): EntryTranslateResult

    suspend fun deleteTranslation(entry: Entry, chapters: List<EntryChapter>)
}

sealed interface EntryTranslateResult {
    data object Translated : EntryTranslateResult

    data class Failed(val failure: EntryTranslateFailure) : EntryTranslateResult
}

val EntryTranslateCapability = entryInteractionCapability<EntryTranslateProcessor>(
    id = CapabilityId("entry.translate"),
)
