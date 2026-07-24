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
    fun `alphabetical sort is case insensitive`() {
        val a = key(id = 1, title = "Banana")
        val b = key(id = 2, title = "apple")
        val sort = LibrarySort(LibrarySort.Type.Alphabetical, LibrarySort.Direction.Ascending)
        sortIds(sort, a, b) shouldBe listOf(2L, 1L)
    }

    @Test
    fun `unread count ascending pushes zeros to end`() {
        val a = key(id = 1, title = "A", unreadCount = 0)
        val b = key(id = 2, title = "B", unreadCount = 5)
        val c = key(id = 3, title = "C", unreadCount = 3)
        val sort = LibrarySort(LibrarySort.Type.UnreadCount, LibrarySort.Direction.Ascending)
        sortIds(sort, a, b, c) shouldBe listOf(3L, 2L, 1L)
    }

    @Test
    fun `unread count descending pushes zeros to end`() {
        val a = key(id = 1, title = "A", unreadCount = 0)
        val b = key(id = 2, title = "B", unreadCount = 5)
        val c = key(id = 3, title = "C", unreadCount = 3)
        val sort = LibrarySort(LibrarySort.Type.UnreadCount, LibrarySort.Direction.Descending)
        sortIds(sort, a, b, c) shouldBe listOf(2L, 3L, 1L)
    }

    @Test
    fun `unread count ties broken by title`() {
        val a = key(id = 1, title = "Z", unreadCount = 5)
        val b = key(id = 2, title = "A", unreadCount = 5)
        val sort = LibrarySort(LibrarySort.Type.UnreadCount, LibrarySort.Direction.Ascending)
        sortIds(sort, a, b) shouldBe listOf(2L, 1L)
    }

    @Test
    fun `tracker mean uses score from key first`() {
        val a = key(id = 1, title = "A").copy(trackerScore = 8.0)
        val b = key(id = 2, title = "B").copy(trackerScore = 3.0)
        val sort = LibrarySort(LibrarySort.Type.TrackerMean, LibrarySort.Direction.Ascending)
        sortIds(sort, a, b) shouldBe listOf(2L, 1L)
    }

    @Test
    fun `tracker mean falls back to map score`() {
        val a = key(id = 1, title = "A")
        val b = key(id = 2, title = "B")
        val sort = LibrarySort(LibrarySort.Type.TrackerMean, LibrarySort.Direction.Ascending)
        val scores = mapOf(1L to 9.0, 2L to 2.0)
        listOf(a, b).sortedWith(librarySortComparator(sort, trackerScores = scores)).map(LibrarySortKey::id) shouldBe
            listOf(2L, 1L)
    }

    @Test
    fun `total chapters sort`() {
        val a = key(id = 1, title = "A", totalEntries = 10)
        val b = key(id = 2, title = "B", totalEntries = 5)
        val sort = LibrarySort(LibrarySort.Type.TotalChapters, LibrarySort.Direction.Descending)
        sortIds(sort, a, b) shouldBe listOf(1L, 2L)
    }

    @Test
    fun `last read sort`() {
        val a = key(id = 1, title = "A", lastRead = 100)
        val b = key(id = 2, title = "B", lastRead = 500)
        val sort = LibrarySort(LibrarySort.Type.LastRead, LibrarySort.Direction.Ascending)
        sortIds(sort, a, b) shouldBe listOf(1L, 2L)
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
