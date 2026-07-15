package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryConsumptionProcessor
import mihon.entry.interactions.consumptionStatus
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class BookConsumptionProcessor(
    private val entryProgressRepository: EntryProgressRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : EntryConsumptionProcessor {
    override val type = EntryType.BOOK
    override val supportsBookmark = false

    override suspend fun setConsumed(entry: Entry, chapters: List<EntryChapter>, consumed: Boolean) {
        entry.requireBook()
        val chaptersToUpdate = chapters.filter { canSetConsumed(it.consumptionStatus(), consumed) }
        if (chaptersToUpdate.isEmpty()) return

        val timestamp = now()
        val chapterIds = chaptersToUpdate.mapTo(mutableSetOf(), EntryChapter::id)
        val progressStates = chaptersToUpdate
            .map(EntryChapter::entryId)
            .distinct()
            .flatMap { entryProgressRepository.getByEntryId(it) }
            .filter { it.chapterId in chapterIds }

        check(entryChapterRepository.updateAll(chaptersToUpdate.map { it.copy(read = consumed) })) {
            "Failed to update BOOK consumption state"
        }
        progressStates.forEach { current ->
            entryProgressRepository.mergeAndSyncChild(
                current.copy(
                    completed = consumed,
                    completionUpdatedAt = timestamp,
                ),
            )
        }
    }

    override suspend fun setBookmarked(entry: Entry, chapters: List<EntryChapter>, bookmarked: Boolean) {
        entry.requireBook()
    }
}
