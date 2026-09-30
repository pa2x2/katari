package mihon.entry.interactions.translate

import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import mihon.entry.interactions.runtime.combinedLatestUnit
import mihon.entry.interactions.runtime.requireProcessor
import mihon.entry.interactions.translation.EntryTranslationLanguageChoices
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/** Stored translations across the entry types that provide them. */
interface EntryTranslateInteraction {
    val types: Set<EntryType>

    /** Emits when stored translations of any type were written or deleted. */
    val changes: Flow<Unit>

    suspend fun prepare(entry: Entry, languages: EntryTranslationLanguageChoices): EntryTranslatePreparation

    suspend fun translatedChapters(entry: Entry, chapters: List<EntryChapter>): Set<Long>

    suspend fun translate(
        entry: Entry,
        chapter: EntryChapter,
        setup: EntryTranslateSetup,
        onProgress: (EntryTranslateProgress) -> Unit,
    ): EntryTranslateResult

    suspend fun deleteTranslation(entry: Entry, chapters: List<EntryChapter>)
}

internal class EntryTranslateInteractionDispatch(
    private val processors: Map<EntryType, EntryTranslateProcessor>,
) : EntryTranslateInteraction {
    override val types: Set<EntryType> = processors.keys

    override val changes: Flow<Unit> = processors.values.map { it.changes }.combinedLatestUnit()

    override suspend fun prepare(entry: Entry, languages: EntryTranslationLanguageChoices): EntryTranslatePreparation =
        processors.requireProcessor("prepare translation", entry.type).prepare(entry, languages)

    override suspend fun translatedChapters(entry: Entry, chapters: List<EntryChapter>): Set<Long> =
        processors[entry.type]?.translatedChapters(entry, chapters).orEmpty()

    override suspend fun translate(
        entry: Entry,
        chapter: EntryChapter,
        setup: EntryTranslateSetup,
        onProgress: (EntryTranslateProgress) -> Unit,
    ): EntryTranslateResult =
        processors.requireProcessor("translate", entry.type).translate(entry, chapter, setup, onProgress)

    override suspend fun deleteTranslation(entry: Entry, chapters: List<EntryChapter>) {
        processors[entry.type]?.deleteTranslation(entry, chapters)
    }
}
