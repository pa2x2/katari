package mihon.entry.interactions.book

import eu.kanade.tachiyomi.source.entry.EntryType
import mihon.entry.interactions.EntryConsumptionProcessor
import mihon.entry.interactions.consumptionStatus
import mihon.entry.interactions.shouldChangeConsumption
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.EntryProgressLocator
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.repository.EntryProgressRepository

internal class BookConsumptionProcessor(
    private val entryProgressRepository: EntryProgressRepository,
    private val entryChapterRepository: EntryChapterRepository,
    private val now: () -> Long = System::currentTimeMillis,
) : EntryConsumptionProcessor {
    override val type = EntryType.BOOK

    override suspend fun setConsumed(
        entry: Entry,
        chapters: List<EntryChapter>,
        consumed: Boolean,
    ): List<EntryChapter> {
        entry.requireBook()
        val chapterIds = chapters.mapTo(mutableSetOf(), EntryChapter::id)
        val progressStates = chapters
            .map(EntryChapter::entryId)
            .distinct()
            .flatMap { entryProgressRepository.getByEntryId(it) }
            .filter { it.chapterId in chapterIds }
        val progressStatesByChapterId = progressStates.groupBy { it.chapterId }
        val chaptersToUpdate = chapters.filter { chapter ->
            shouldChangeConsumption(
                chapter.consumptionStatus(
                    hasPartialProgress = progressStatesByChapterId[chapter.id]
                        .orEmpty()
                        .any { it.hasPartialBookProgress },
                ),
                consumed,
            )
        }
        if (chaptersToUpdate.isEmpty()) return emptyList()

        val timestamp = now()
        val chapterIdsToUpdate = chaptersToUpdate.mapTo(mutableSetOf(), EntryChapter::id)

        check(entryChapterRepository.updateAll(chaptersToUpdate.map { it.copy(read = consumed) })) {
            "Failed to update BOOK consumption state"
        }
        progressStates.filter { it.chapterId in chapterIdsToUpdate }.forEach { current ->
            entryProgressRepository.mergeAndSyncChild(
                current.copy(
                    locator = if (consumed) {
                        current.locator
                    } else {
                        EntryProgressLocator(kind = BOOK_PROGRESS_LOCATOR_KIND)
                    },
                    completed = consumed,
                    locatorUpdatedAt = if (consumed) current.locatorUpdatedAt else timestamp,
                    completionUpdatedAt = timestamp,
                ),
            )
        }

        return chaptersToUpdate
    }
}
