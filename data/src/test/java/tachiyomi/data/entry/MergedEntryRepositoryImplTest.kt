package tachiyomi.data.entry

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows

class MergedEntryRepositoryImplTest {

    @Test
    fun `merge group validation accepts same type entries from the active profile`() {
        assertDoesNotThrow {
            validateMergeGroupEntries(
                orderedEntryIds = listOf(1L, 2L),
                entries = listOf(1L to "manga", 2L to "manga"),
            )
        }
    }

    @Test
    fun `merge group validation rejects mixed entry types`() {
        assertThrows<IllegalArgumentException> {
            validateMergeGroupEntries(
                orderedEntryIds = listOf(1L, 2L),
                entries = listOf(1L to "manga", 2L to "anime"),
            )
        }
    }

    @Test
    fun `merge group validation rejects entries outside the active profile`() {
        assertThrows<IllegalArgumentException> {
            validateMergeGroupEntries(
                orderedEntryIds = listOf(1L, 2L),
                entries = listOf(1L to "manga"),
            )
        }
    }
}
