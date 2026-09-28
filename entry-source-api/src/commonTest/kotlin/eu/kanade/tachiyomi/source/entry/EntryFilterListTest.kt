package eu.kanade.tachiyomi.source.entry

import kotlin.test.Test
import kotlin.test.assertNotEquals

class EntryFilterListTest {

    @Test
    fun `republishing list after mutating a grouped filter is treated as an update`() {
        val nestedFilter = object : EntryFilter.Text("Text") {}
        val group = object : EntryFilter.Group<EntryFilter<*>>("Group", listOf(nestedFilter)) {}
        val filters = EntryFilterList(group)

        nestedFilter.state = "updated"

        assertNotEquals(filters, filters)
        assertNotEquals(filters, EntryFilterList(filters.toList()))
    }
}
