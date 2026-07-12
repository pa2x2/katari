package tachiyomi.domain.history.interactor

import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository
import tachiyomi.domain.entry.service.sortedForReading
import tachiyomi.domain.history.repository.HistoryRepository
import tachiyomi.domain.source.service.HiddenSourceIds

class GetNextChapters(
    private val getEntry: GetEntry,
    private val entryChapterRepository: EntryChapterRepository,
    private val historyRepository: HistoryRepository,
    private val hiddenSourceIds: HiddenSourceIds,
) {

    suspend fun await(onlyUnread: Boolean = true): List<EntryChapter> {
        val history = historyRepository.getLastHistory() ?: return emptyList()
        if (history.coverData.sourceId in hiddenSourceIds.get()) return emptyList()
        return await(history.entryId, history.chapterId, onlyUnread)
    }

    suspend fun await(entryId: Long, onlyUnread: Boolean = true): List<EntryChapter> {
        val entry = getEntry.await(entryId) ?: return emptyList()
        val chapters = entryChapterRepository.getChaptersByEntryIdAwait(entryId, applyScanlatorFilter = true)
            .sortedForReading(entry)

        return if (onlyUnread) {
            chapters.filterNot { it.read }
        } else {
            chapters
        }
    }

    suspend fun await(
        entryId: Long,
        fromChapterId: Long,
        onlyUnread: Boolean = true,
    ): List<EntryChapter> {
        val allChapters = await(entryId, onlyUnread = false)
        val currChapterIndex = allChapters.indexOfFirst { it.id == fromChapterId }
        if (currChapterIndex == -1) {
            return if (onlyUnread) allChapters.filterNot(EntryChapter::read) else allChapters
        }

        val currentOrFollowing = allChapters.drop(currChapterIndex)

        if (onlyUnread) {
            return currentOrFollowing.filterNot(EntryChapter::read)
        }

        // The "next chapter" is either:
        // - The current chapter if it isn't completely read
        // - The chapters after the current chapter if the current one is completely read
        val fromChapter = allChapters[currChapterIndex]
        return if (!fromChapter.read) {
            currentOrFollowing
        } else {
            currentOrFollowing.drop(1)
        }
    }
}
