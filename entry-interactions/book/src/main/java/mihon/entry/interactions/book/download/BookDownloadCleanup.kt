package mihon.entry.interactions.book.download

import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.sortedForReading

internal class BookDownloadCleanup(
    private val downloadPreferences: DownloadPreferences,
    private val getCategories: GetCategories,
    private val getEntryWithChapters: GetEntryWithChapters,
    private val entryRepository: EntryRepository,
    private val downloadManager: BookDownloadManager,
) {
    suspend fun afterMarkedConsumed(entry: Entry, chapters: List<EntryChapter>) {
        if (!downloadPreferences.removeAfterMarkedAsRead.get()) return
        deleteEligible(entry, chapters)
    }

    suspend fun afterReaderCompleted(visibleEntry: Entry, currentChapter: EntryChapter) {
        val removeAfterReadSlots = downloadPreferences.removeAfterReadSlots.get()
        if (removeAfterReadSlots < 0) return

        val readingOrder = getEntryWithChapters.awaitChapters(visibleEntry.id).sortedForReading(visibleEntry)
        val currentIndex = readingOrder.indexOfFirst { it.id == currentChapter.id }
        if (currentIndex < 0) return
        val chapterToDelete = readingOrder.getOrNull(currentIndex - removeAfterReadSlots) ?: return
        val owner = if (chapterToDelete.entryId == visibleEntry.id) {
            visibleEntry
        } else {
            entryRepository.getEntryById(chapterToDelete.entryId)
        } ?: return
        if (owner.type != visibleEntry.type) return

        deleteEligible(owner, listOf(chapterToDelete))
    }

    private suspend fun deleteEligible(entry: Entry, chapters: List<EntryChapter>) {
        if (isExcluded(entry)) return
        val chaptersToDelete = if (downloadPreferences.removeBookmarkedChapters.get()) {
            chapters
        } else {
            chapters.filterNot { it.bookmark }
        }
        if (chaptersToDelete.isNotEmpty()) {
            downloadManager.delete(entry, chaptersToDelete)
        }
    }

    private suspend fun isExcluded(entry: Entry): Boolean {
        val excludedCategoryIds = downloadPreferences.removeExcludeCategories.get()
            .mapNotNull(String::toLongOrNull)
            .toSet()
        val entryCategoryIds = getCategories.await(entry.id)
            .map { it.id }
            .ifEmpty { listOf(0L) }
        return entryCategoryIds.any { it in excludedCategoryIds }
    }
}
