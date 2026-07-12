package eu.kanade.tachiyomi.source.entry

import kotlin.test.Test
import kotlin.test.assertNotEquals

class EntryFilterListTest {

    @Test
    fun `republishing list after mutating any interactive filter is treated as an update`() {
        interactiveFilters().forEach { filter ->
            val filters = EntryFilterList(filter)

            mutate(filter)

            assertNotEquals(filters, filters)
            assertNotEquals(filters, EntryFilterList(filters.toList()))
        }
    }

    @Test
    fun `republishing list after mutating a grouped filter is treated as an update`() {
        val nestedFilter = textFilter()
        val group = object : EntryFilter.Group<EntryFilter<*>>("Group", listOf(nestedFilter)) {}
        val filters = EntryFilterList(group)

        nestedFilter.state = "updated"

        assertNotEquals(filters, filters)
        assertNotEquals(filters, EntryFilterList(filters.toList()))
    }

    private fun interactiveFilters(): List<EntryFilter<*>> = listOf(
        textFilter(),
        object : EntryFilter.CheckBox("Check box") {},
        object : EntryFilter.TriState("Tri-state") {},
        object : EntryFilter.Select<String>("Select", arrayOf("First", "Second")) {},
        object : EntryFilter.Sort("Sort", arrayOf("Name")) {},
    )

    private fun textFilter() = object : EntryFilter.Text("Text") {}

    private fun mutate(filter: EntryFilter<*>) {
        when (filter) {
            is EntryFilter.Text -> filter.state = "updated"
            is EntryFilter.CheckBox -> filter.state = true
            is EntryFilter.TriState -> filter.state = EntryFilter.TriState.STATE_INCLUDE
            is EntryFilter.Select<*> -> filter.state = 1
            is EntryFilter.Sort -> filter.state = EntryFilter.Sort.Selection(index = 0, ascending = true)
            else -> error("Unsupported interactive filter: ${filter::class.simpleName}")
        }
    }
}
