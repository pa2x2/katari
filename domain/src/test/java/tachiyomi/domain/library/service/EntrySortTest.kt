package tachiyomi.domain.library.service

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class EntrySortTest {

    private data class TestEntry(
        val id: Long,
        val number: Double,
        val dateUpload: Long,
        val name: String,
        val url: String,
        val sourceOrder: Long,
    )

    private companion object {
        const val SORTING_SOURCE = 0L
        const val SORTING_NUMBER = 0x100L
        const val SORTING_UPLOAD_DATE = 0x200L
        const val SORTING_ALPHABET = 0x300L
    }

    private fun comparator(
        sorting: Long,
        sortDescending: Boolean,
    ) = entrySortComparator(
        sorting = sorting,
        sortDescending = sortDescending,
        sortingSourceFlag = SORTING_SOURCE,
        sortingNumberFlag = SORTING_NUMBER,
        sortingUploadDateFlag = SORTING_UPLOAD_DATE,
        sortingAlphabetFlag = SORTING_ALPHABET,
        numberSelector = TestEntry::number,
        dateUploadSelector = TestEntry::dateUpload,
        nameSelector = TestEntry::name,
        urlSelector = TestEntry::url,
        sourceOrderSelector = TestEntry::sourceOrder,
    )

    @Test
    fun `source sort ascending puts the oldest source item first`() {
        val entries = listOf(
            TestEntry(id = 1, number = 1.0, dateUpload = 100, name = "A", url = "/1", sourceOrder = 0),
            TestEntry(id = 2, number = 2.0, dateUpload = 200, name = "B", url = "/2", sourceOrder = 1),
        )
        entries.sortedWith(comparator(SORTING_SOURCE, sortDescending = false)).map(TestEntry::id) shouldBe
            listOf(2L, 1L)
        entries.sortedWith(comparator(SORTING_SOURCE, sortDescending = true)).map(TestEntry::id) shouldBe listOf(1L, 2L)
    }

    @Test
    fun `unknown numbers and upload dates stay last in both directions`() {
        val entries = listOf(
            TestEntry(id = 1, number = -1.0, dateUpload = 0, name = "A", url = "/1", sourceOrder = 0),
            TestEntry(id = 2, number = 2.0, dateUpload = 100, name = "B", url = "/2", sourceOrder = 1),
            TestEntry(id = 3, number = 3.0, dateUpload = 200, name = "C", url = "/3", sourceOrder = 2),
        )
        for (sorting in listOf(SORTING_NUMBER, SORTING_UPLOAD_DATE)) {
            entries.sortedWith(comparator(sorting, sortDescending = false)).map(TestEntry::id) shouldBe
                listOf(2L, 3L, 1L)
            entries.sortedWith(comparator(sorting, sortDescending = true)).map(TestEntry::id) shouldBe
                listOf(3L, 2L, 1L)
        }
    }

    @Test
    fun `alphabet sort falls back to url for blank names and to source order for ties`() {
        val blankName = listOf(
            TestEntry(id = 1, number = 1.0, dateUpload = 100, name = "", url = "/b", sourceOrder = 0),
            TestEntry(id = 2, number = 2.0, dateUpload = 200, name = "A", url = "/a", sourceOrder = 1),
        )
        val tied = listOf(
            TestEntry(id = 2, number = 1.0, dateUpload = 100, name = "A", url = "/1", sourceOrder = 1),
            TestEntry(id = 1, number = 1.0, dateUpload = 200, name = "A", url = "/2", sourceOrder = 0),
        )
        blankName.sortedWith(comparator(SORTING_ALPHABET, sortDescending = false)).map(TestEntry::id) shouldBe
            listOf(1L, 2L)
        tied.sortedWith(comparator(SORTING_ALPHABET, sortDescending = false)).map(TestEntry::id) shouldBe
            listOf(1L, 2L)
    }
}
