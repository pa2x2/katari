package eu.kanade.tachiyomi.source.entry.filter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EntryPartialDateTest {
    @Test
    fun `calendar validation rejects impossible dates including century leap years`() {
        listOf("0000", "2023-02-29", "1900-02-29", "2024-04-31", "2024-00", "2024-13", "24", "2024-02-").forEach {
            assertNull(EntryPartialDate.parse(it), it)
        }
        assertEquals(EntryPartialDate(2000, 2, 29), EntryPartialDate.parse("2000-02-29"))
    }
}
