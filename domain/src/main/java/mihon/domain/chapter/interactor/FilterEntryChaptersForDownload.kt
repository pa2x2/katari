package mihon.domain.chapter.interactor

import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.download.service.DownloadPreferences
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.repository.EntryChapterRepository

/**
 * Interactor responsible for determining which EntryChapters should be downloaded.
 *
 * @property entryChapterRepository Repository for retrieving chapters by entry ID.
 * @property downloadPreferences User preferences related to EntryChapter downloads.
 * @property getCategories Interactor for retrieving categories associated with an entry.
 */
class FilterEntryChaptersForDownload(
    private val entryChapterRepository: EntryChapterRepository,
    private val downloadPreferences: DownloadPreferences,
    private val getCategories: GetCategories,
) {

    /**
     * Determines which EntryChapters should be downloaded based on user preferences.
     *
     * @param entry The entry for which EntryChapters may be downloaded.
     * @param newChapters The list of new EntryChapters available for the entry.
     * @return A list of EntryChapters that should be downloaded.
     */
    suspend fun await(entry: Entry, newChapters: List<EntryChapter>): List<EntryChapter> {
        if (
            newChapters.isEmpty() ||
            !downloadPreferences.downloadNewEntryChapters.get() ||
            !entry.shouldDownloadNewEntryChapters()
        ) {
            return emptyList()
        }

        if (!downloadPreferences.downloadNewUnreadEntryChaptersOnly.get()) return newChapters

        val readChapterNumbers = entryChapterRepository.getChaptersByEntryIdAwait(entry.id)
            .asSequence()
            .filter { it.read && it.isRecognizedNumber }
            .map { it.chapterNumber }
            .toSet()

        return newChapters.filterNot { it.chapterNumber in readChapterNumbers }
    }

    /**
     * Determines whether new EntryChapters should be downloaded for the entry based on user preferences and the
     * categories to which the entry belongs.
     *
     * @return `true` if EntryChapters of the entry should be downloaded.
     */
    private suspend fun Entry.shouldDownloadNewEntryChapters(): Boolean {
        if (!favorite) return false

        val categories = getCategories.await(id).map { it.id }.ifEmpty { listOf(DEFAULT_CATEGORY_ID) }
        val includedCategories = downloadPreferences.downloadNewEntryChapterCategories.get().map { it.toLong() }
        val excludedCategories = downloadPreferences.downloadNewEntryChapterCategoriesExclude.get().map { it.toLong() }

        return when {
            // Default Download from all categories
            includedCategories.isEmpty() && excludedCategories.isEmpty() -> true
            // In excluded category
            categories.any { it in excludedCategories } -> false
            // Included category not selected
            includedCategories.isEmpty() -> true
            // In included category
            else -> categories.any { it in includedCategories }
        }
    }

    companion object {
        private const val DEFAULT_CATEGORY_ID = 0L
    }
}
