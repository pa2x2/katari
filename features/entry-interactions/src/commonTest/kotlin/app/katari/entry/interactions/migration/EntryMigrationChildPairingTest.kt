package app.katari.entry.interactions.migration

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.migration.MigrationChildStateTransfer
import tachiyomi.domain.entry.model.EntryChapter

class EntryMigrationChildPairingTest {
    /**
     * Matched by number, a second season's second episode marked the second episode of an entry listing every season
     * as watched, instead of the one it is in that list.
     */
    @Test
    fun `a season lands on its own stretch of the target, read up to its last watched episode`() {
        val seasonTwo = episodes(entryId = 2L, firstId = 21L, count = 3, watched = setOf(22L))
        val allSeasons = episodes(entryId = 9L, firstId = 101L, count = 6)

        val updates = prepareMigrationChildUpdates(
            sourceChildren = seasonTwo,
            childPairs = EntryMigrationChildPairing.ByPosition(offset = 3).pair(seasonTwo, allSeasons),
            transfer = MigrationChildStateTransfer(read = true, bookmarks = true),
        )

        updates.map { it.updated.id to it.updated.read } shouldBe listOf(104L to true, 105L to true)
    }

    /** Episodes numbered from one, listed newest first as sources list them. */
    private fun episodes(entryId: Long, firstId: Long, count: Int, watched: Set<Long> = emptySet()) =
        (count - 1 downTo 0).map { index ->
            val id = firstId + index
            EntryChapter.create().copy(
                id = id,
                entryId = entryId,
                url = "/$entryId/$index",
                chapterNumber = index + 1.0,
                sourceOrder = (count - 1 - index).toLong(),
                read = id in watched,
            )
        }
}
