package eu.kanade.domain.chapter.model

import mihon.entry.interactions.EntryDownloadInteraction
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.service.getChapterSort
import tachiyomi.domain.util.applyFilter
import tachiyomi.source.local.LocalSource

/**
 * Applies the view filters to the list of chapters obtained from the database.
 * @return an observable of the list of chapters filtered and sorted.
 */
fun List<EntryChapter>.applyFilters(
    entry: Entry,
    entryDownloadInteraction: EntryDownloadInteraction,
): List<EntryChapter> {
    val isLocalEntry = entry.source == LocalSource.ID
    val unreadFilter = entry.unreadFilter
    val downloadedFilter = entry.downloadedFilter
    val bookmarkedFilter = entry.bookmarkedFilter

    return filter { chapter -> applyFilter(unreadFilter) { !chapter.read } }
        .filter { chapter -> applyFilter(bookmarkedFilter) { chapter.bookmark } }
        .filter { chapter ->
            applyFilter(downloadedFilter) {
                val downloaded = entryDownloadInteraction.isDownloaded(entry, chapter)
                downloaded || isLocalEntry
            }
        }
        .sortedWith(getChapterSort(entry))
}
