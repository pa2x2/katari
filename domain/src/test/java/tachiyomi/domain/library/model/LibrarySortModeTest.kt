package tachiyomi.domain.library.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.category.model.Category

class LibrarySortModeTest {

    @Test
    fun `effective sort uses category flags for regular categories`() {
        val globalSort = LibrarySort(LibrarySort.Type.LastRead, LibrarySort.Direction.Descending)
        val category = Category(
            id = 1L,
            name = "Favorites",
            order = 0L,
            flags = LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Ascending).flag,
        )

        category.effectiveLibrarySort(globalSort) shouldBe LibrarySort(
            LibrarySort.Type.DateAdded,
            LibrarySort.Direction.Ascending,
        )
    }

    @Test
    fun `effective sort falls back to global sort for system category`() {
        val globalSort = LibrarySort(LibrarySort.Type.LastRead, LibrarySort.Direction.Descending)
        val category = Category(
            id = Category.UNCATEGORIZED_ID,
            name = "",
            order = 0L,
            flags = LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Ascending).flag,
        )

        category.effectiveLibrarySort(globalSort) shouldBe globalSort
    }

    @Test
    fun `effective sort falls back to global sort without category`() {
        val globalSort = LibrarySort(LibrarySort.Type.UnreadCount, LibrarySort.Direction.Ascending)

        null.effectiveLibrarySort(globalSort) shouldBe globalSort
    }
}
