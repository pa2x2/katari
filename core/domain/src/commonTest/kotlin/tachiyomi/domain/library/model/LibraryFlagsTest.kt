package tachiyomi.domain.library.model

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LibraryFlagsTest {

    @Test
    fun `persisted sort flag bits replace the previous sort without leaking old bits`() {
        val currentSort = LibrarySort(
            LibrarySort.Type.UnreadCount,
            LibrarySort.Direction.Descending,
        )
        currentSort.flag shouldBe 0b00001100

        val sort = LibrarySort(LibrarySort.Type.DateAdded, LibrarySort.Direction.Ascending)

        (currentSort.flag + sort) shouldBe 0b01011100
    }
}
