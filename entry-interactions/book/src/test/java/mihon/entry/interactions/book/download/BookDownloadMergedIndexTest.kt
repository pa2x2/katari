package mihon.entry.interactions.book.download

import io.mockk.mockk
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryMerge
import kotlin.test.assertEquals

class BookDownloadMergedIndexTest {
    @Test
    fun `each merged member resolves the complete group`() {
        val cache = BookDownloadCache(mockk())

        cache.updateMergedEntries(
            listOf(
                EntryMerge(targetId = 1L, entryId = 1L, position = 0L),
                EntryMerge(targetId = 1L, entryId = 2L, position = 1L),
                EntryMerge(targetId = 1L, entryId = 3L, position = 2L),
            ),
        )

        assertEquals(setOf(1L, 2L, 3L), cache.memberEntryIds(1L))
        assertEquals(setOf(1L, 2L, 3L), cache.memberEntryIds(2L))
        assertEquals(setOf(1L, 2L, 3L), cache.memberEntryIds(3L))
        assertEquals(setOf(9L), cache.memberEntryIds(9L))
    }
}
