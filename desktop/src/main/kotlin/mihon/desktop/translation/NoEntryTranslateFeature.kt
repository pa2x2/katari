package mihon.desktop.translation

import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.entry.interactions.translate.EntryTranslateFeature
import mihon.entry.interactions.translate.EntryTranslatePreparation
import mihon.entry.interactions.translate.EntryTranslateQueueItem
import mihon.entry.interactions.translate.EntryTranslateSetup
import mihon.entry.interactions.translate.EntryTranslateStatus
import mihon.entry.interactions.translate.EntryTranslateWaiting
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter

/**
 * Background chapter translation as a feature that applies to no entry type, so screens hide the translation queue and
 * every translate action, as they do for a type that does not translate. Stands in for background translation, which
 * needs translation and text recognition engines and background work desktop does not have yet; a desktop
 * implementation of that feature replaces this once it does.
 */
@Inject
class NoEntryTranslateFeature : EntryTranslateFeature {
    override fun isApplicable(type: EntryType): Boolean = false

    override val queue: Flow<List<EntryTranslateQueueItem>> = flowOf(emptyList())

    override val waiting: Flow<EntryTranslateWaiting?> = flowOf(null)

    override val paused: Flow<Boolean> = flowOf(false)

    override fun pause() = Unit

    override fun resume() = Unit

    override suspend fun prepare(entry: Entry): EntryTranslatePreparation? = null

    override fun observeStatuses(entry: Entry): Flow<Map<Long, EntryTranslateStatus>> = flowOf(emptyMap())

    override suspend fun translate(
        entry: Entry,
        chapters: List<EntryChapter>,
        setup: EntryTranslateSetup,
        startNow: Boolean,
    ) = Unit

    override suspend fun translateWithCurrentSettings(entry: Entry, chapters: List<EntryChapter>) = Unit

    override suspend fun untranslatedDownloads(entry: Entry): List<EntryChapter> = emptyList()

    override suspend fun startNow(chapterIds: List<Long>) = Unit

    override suspend fun retry(chapterIds: List<Long>) = Unit

    override suspend fun cancel(chapterIds: List<Long>) = Unit

    override suspend fun cancelAll() = Unit

    override suspend fun deleteTranslation(entry: Entry, chapters: List<EntryChapter>) = Unit
}
