package tachiyomi.domain.library.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibrarySort

class LibraryItemSortTest {

    private fun sortIds(sort: LibrarySort, vararg items: LibrarySortKey): List<Long> {
        return items.toList().sortedWith(librarySortComparator(sort)).map(LibrarySortKey::id)
    }

    private fun key(
        id: Long,
        title: String,
        unreadCount: Long = 0,
        lastRead: Long = 0,
        lastUpdate: Long = 0,
        totalEntries: Long = 0,
        latestUpload: Long = 0,
        entryFetchDate: Long = 0,
        dateAdded: Long = 0,
    ) = LibrarySortKey(
        id = id,
        title = title,
        lastRead = lastRead,
        lastUpdate = lastUpdate,
        unreadCount = unreadCount,
        totalEntries = totalEntries,
        latestUpload = latestUpload,
        entryFetchDate = entryFetchDate,
        dateAdded = dateAdded,
        trackerScore = null,
    )

    @Test
    fun `unread count pushes zeros to the end in both directions`() {
        val a = key(id = 1, title = "A", unreadCount = 0)
        val b = key(id = 2, title = "B", unreadCount = 5)
        val c = key(id = 3, title = "C", unreadCount = 3)
        sortIds(LibrarySort(LibrarySort.Type.UnreadCount, LibrarySort.Direction.Ascending), a, b, c) shouldBe
            listOf(3L, 2L, 1L)
        sortIds(LibrarySort(LibrarySort.Type.UnreadCount, LibrarySort.Direction.Descending), a, b, c) shouldBe
            listOf(2L, 3L, 1L)
    }

    @Test
    fun `missing summary sort values remain last in both directions`() {
        val available = key(id = 1L, title = "Available", lastRead = 100L)
        val unavailable = key(id = 2L, title = "Unavailable").copy(lastRead = null)

        sortIds(
            LibrarySort(LibrarySort.Type.LastRead, LibrarySort.Direction.Ascending),
            unavailable,
            available,
        ) shouldBe listOf(1L, 2L)
        sortIds(
            LibrarySort(LibrarySort.Type.LastRead, LibrarySort.Direction.Descending),
            unavailable,
            available,
        ) shouldBe listOf(1L, 2L)
    }
}
