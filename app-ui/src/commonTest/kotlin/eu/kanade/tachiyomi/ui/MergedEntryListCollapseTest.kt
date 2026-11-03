package eu.kanade.tachiyomi.ui

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class MergedEntryListCollapseTest {

    @Test
    fun `merged entries collapse to their newest record while unmerged repeats are preserved`() {
        val records = listOf(
            Record(childId = 31, actualEntryId = 12, visibleEntryId = 10),
            Record(childId = 22, actualEntryId = 21, visibleEntryId = 20),
            Record(childId = 11, actualEntryId = 10, visibleEntryId = 10),
            Record(childId = 21, actualEntryId = 20, visibleEntryId = 20),
            Record(childId = 40, actualEntryId = 40, visibleEntryId = 40),
        )

        val collapsed = records.collapseByVisibleEntry(
            actualEntryId = Record::actualEntryId,
            visibleEntryId = Record::visibleEntryId,
        )

        collapsed.map(Record::childId) shouldBe listOf(31L, 22L, 40L)

        val unmerged = listOf(
            Record(childId = 1, actualEntryId = 10, visibleEntryId = 10),
            Record(childId = 2, actualEntryId = 10, visibleEntryId = 10),
            Record(childId = 3, actualEntryId = 20, visibleEntryId = 20),
        )

        unmerged.collapseByVisibleEntry(
            actualEntryId = Record::actualEntryId,
            visibleEntryId = Record::visibleEntryId,
        ) shouldBe unmerged
    }

    private data class Record(
        val childId: Long,
        val actualEntryId: Long,
        val visibleEntryId: Long,
    )
}
