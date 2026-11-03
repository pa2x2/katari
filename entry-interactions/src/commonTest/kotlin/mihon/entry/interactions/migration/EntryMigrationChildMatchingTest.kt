package mihon.entry.interactions.migration

import io.kotest.matchers.collections.shouldContainExactly
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryChapter

class EntryMigrationChildMatchingTest {
    @Test
    fun `matching preserves source precedence and number versus name rules`() {
        val sourceChildren = listOf(
            child(id = 1, chapterNumber = 1.0, name = "first-number"),
            child(id = 2, chapterNumber = 1.0, name = "duplicate-number"),
            child(id = 3, chapterNumber = 2.0, name = "shared-name"),
            child(id = 4, chapterNumber = -1.0, name = "shared-name"),
            child(id = 5, chapterNumber = 0.0, name = "zero"),
            child(id = 6, chapterNumber = Double.POSITIVE_INFINITY, name = "positive-infinity"),
            child(id = 7, chapterNumber = 4.0, name = "negative-infinity"),
            child(id = 8, chapterNumber = 5.0, name = "numeric-name"),
        )
        val targetChildren = listOf(
            child(id = 101, chapterNumber = 1.0, name = "ignored-name"),
            child(id = 102, chapterNumber = 9.0, name = "shared-name"),
            child(id = 103, chapterNumber = -1.0, name = "shared-name"),
            child(id = 104, chapterNumber = -1.0, name = "missing"),
            child(id = 105, chapterNumber = -0.0, name = "ignored-zero-name"),
            child(id = 106, chapterNumber = Double.NaN, name = "shared-name"),
            child(id = 107, chapterNumber = Double.POSITIVE_INFINITY, name = "ignored-infinity-name"),
            child(id = 108, chapterNumber = Double.NEGATIVE_INFINITY, name = "negative-infinity"),
            child(id = 109, chapterNumber = -1.0, name = "numeric-name"),
        )

        val matches = matchMigrationChildren(sourceChildren, targetChildren)

        matches.map { it.target.id }.shouldContainExactly(101L, 102L, 103L, 104L, 105L, 106L, 107L, 108L, 109L)
        matches.map { it.source?.id }.shouldContainExactly(1L, null, 3L, null, 5L, 3L, 6L, 7L, 8L)
    }

    private fun child(id: Long, chapterNumber: Double, name: String): EntryChapter {
        return EntryChapter.create().copy(
            id = id,
            entryId = id / 100,
            url = "child-$id",
            name = name,
            chapterNumber = chapterNumber,
        )
    }
}
