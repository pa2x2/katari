package tachiyomi.domain.library.service

import tachiyomi.core.common.util.lang.compareToWithCollator
import tachiyomi.domain.library.model.LibrarySort

data class LibrarySortKey(
    val id: Long,
    val title: String,
    val lastRead: Long?,
    val lastUpdate: Long,
    val unreadCount: Long?,
    val totalEntries: Long?,
    val latestUpload: Long,
    val entryFetchDate: Long,
    val dateAdded: Long,
)

fun librarySortComparator(sort: LibrarySort): Comparator<LibrarySortKey> {
    // The collator already ignores case
    val sortAlphabetically: (LibrarySortKey, LibrarySortKey) -> Int = { item1, item2 ->
        item1.title.compareToWithCollator(item2.title)
    }

    val comparator = Comparator<LibrarySortKey> { item1, item2 ->
        when (sort.type) {
            LibrarySort.Type.Alphabetical -> {
                sortAlphabetically(item1, item2)
            }
            LibrarySort.Type.LastRead -> {
                compareNullable(item1.lastRead, item2.lastRead, sort.isAscending)
            }
            LibrarySort.Type.LastUpdate -> {
                item1.lastUpdate.compareTo(item2.lastUpdate)
            }
            LibrarySort.Type.UnreadCount -> when {
                item1.unreadCount == item2.unreadCount -> 0
                item1.unreadCount == null -> if (sort.isAscending) 1 else -1
                item2.unreadCount == null -> if (sort.isAscending) -1 else 1
                item1.unreadCount == 0L -> if (sort.isAscending) 1 else -1
                item2.unreadCount == 0L -> if (sort.isAscending) -1 else 1
                else -> item1.unreadCount.compareTo(item2.unreadCount)
            }
            LibrarySort.Type.TotalChapters -> {
                compareNullable(item1.totalEntries, item2.totalEntries, sort.isAscending)
            }
            LibrarySort.Type.LatestChapter -> {
                item1.latestUpload.compareTo(item2.latestUpload)
            }
            LibrarySort.Type.ChapterFetchDate -> {
                item1.entryFetchDate.compareTo(item2.entryFetchDate)
            }
            LibrarySort.Type.DateAdded -> {
                item1.dateAdded.compareTo(item2.dateAdded)
            }
            LibrarySort.Type.Random -> {
                error("Random sort should be handled before calling librarySortComparator")
            }
        }
    }

    return comparator
        .let { if (sort.isAscending) it else it.reversed() }
        .thenComparator(sortAlphabetically)
}

private fun <T : Comparable<T>> compareNullable(first: T?, second: T?, ascending: Boolean): Int {
    return when {
        first == null && second == null -> 0
        first == null -> if (ascending) 1 else -1
        second == null -> if (ascending) -1 else 1
        else -> first.compareTo(second)
    }
}
