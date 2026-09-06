package eu.kanade.tachiyomi.source.entry.filter

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EntryPartialDateTest {
    @Test
    fun `partial dates preserve supplied precision`() {
        assertEquals(EntryPartialDate(2024), EntryPartialDate.parse("2024"))
        assertEquals("2024-02", EntryPartialDate.parse("2024-2").toString())
        assertEquals("2024-02-29", EntryPartialDate.parse("2024-2-29").toString())
    }

    @Test
    fun `calendar validation rejects impossible dates including century leap years`() {
        listOf("0000", "2023-02-29", "1900-02-29", "2024-04-31", "2024-00", "2024-13", "24", "2024-02-").forEach {
            assertNull(EntryPartialDate.parse(it), it)
        }
        assertEquals(EntryPartialDate(2000, 2, 29), EntryPartialDate.parse("2000-02-29"))
    }

    @Test
    fun `partial comparison spans the complete period without changing value`() {
        val year = EntryPartialDate(2024)
        assertEquals(20240101, year.earliestDayKey())
        assertEquals(20241231, year.latestDayKey())
        assertEquals(20240229, EntryPartialDate(2024, 2).latestDayKey())
        assertEquals("2024", year.toString())
    }

    @Test
    fun `date filter distinguishes blank drafts invalid input precision and bounds`() {
        val filter =
            EntryDateFilter(
                "Date",
                EntryFilterMetadata(id = "date"),
                allowedPrecisions = setOf(EntryDatePrecision.MONTH),
                minimum = EntryPartialDate(2020),
            )
        assertTrue(filter.validateFilter().isEmpty())
        filter.state = "2024-02-31"
        assertEquals(EntryFilterValidationCode.INVALID_DATE, filter.validateFilter().single().code)
        filter.state = "2024"
        assertEquals(EntryFilterValidationCode.DATE_PRECISION, filter.validateFilter().single().code)
        filter.state = "2019-12"
        assertEquals(EntryFilterValidationCode.DATE_BOUNDS, filter.validateFilter().single().code)
        filter.dateValue = EntryPartialDate(2020, 1)
        assertTrue(filter.validateFilter().isEmpty())
    }
}
