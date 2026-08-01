package eu.kanade.tachiyomi.source.entry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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

    @Test
    fun `autocomplete options provide safe defaults and reject invalid request policies`() {
        assertEquals(
            EntryFilterAutocompleteOptions(),
            EntryFilterAutocompleteOptions(
                debounceMillis = EntryFilterAutocompleteOptions.DEFAULT_DEBOUNCE_MILLIS,
                minimumQueryLength = EntryFilterAutocompleteOptions.DEFAULT_MINIMUM_QUERY_LENGTH,
                requestOnFocus = false,
                maximumResults = EntryFilterAutocompleteOptions.DEFAULT_MAXIMUM_RESULTS,
            ),
        )

        assertFailsWith<IllegalArgumentException> {
            EntryFilterAutocompleteOptions(debounceMillis = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            EntryFilterAutocompleteOptions(minimumQueryLength = -1)
        }
        assertFailsWith<IllegalArgumentException> {
            EntryFilterAutocompleteOptions(maximumResults = 0)
        }
    }

    @Test
    fun `paged group policies and page requests reject invalid bounds`() {
        assertEquals(50, EntryFilterPagingOptions().pageSize)
        assertEquals(300L, EntryFilterPagingOptions().search?.debounceMillis)

        assertFailsWith<IllegalArgumentException> { EntryFilterPagingOptions(pageSize = 0) }
        assertFailsWith<IllegalArgumentException> { EntryFilterPagingOptions(pageSize = 201) }
        assertFailsWith<IllegalArgumentException> {
            EntryFilterPagingSearchOptions(minimumQueryLength = 0)
        }
        assertFailsWith<IllegalArgumentException> {
            EntryFilterPageRequest(
                scope = EntryFilterPageScope.AVAILABLE,
                query = null,
                continuationToken = "next",
                requestedSize = 50,
                reason = EntryFilterPageLoadReason.INITIAL,
            )
        }
    }

    private fun interactiveFilters(): List<EntryFilter<*>> = listOf(
        textFilter(),
        autocompleteFilter(),
        object : EntryFilter.CheckBox("Check box") {},
        object : EntryFilter.TriState("Tri-state") {},
        object : EntryFilter.Select<String>("Select", arrayOf("First", "Second")) {},
        object : EntryFilter.Sort("Sort", arrayOf("Name")) {},
    )

    private fun textFilter() = object : EntryFilter.Text("Text") {}

    private fun autocompleteFilter() = object : EntryFilter.Autocomplete("Autocomplete") {
        override fun getSuggestionQuery(input: EntryFilterTextInput): String = input.text

        override suspend fun getSuggestions(
            input: EntryFilterTextInput,
            query: String,
        ): List<EntryFilterSuggestion> = emptyList()

        override fun applySuggestion(
            input: EntryFilterTextInput,
            suggestion: EntryFilterSuggestion,
        ): EntryFilterTextEdit = EntryFilterTextEdit(suggestion.value, suggestion.value.length)
    }

    private fun mutate(filter: EntryFilter<*>) {
        when (filter) {
            is EntryFilter.Text -> filter.state = "updated"
            is EntryFilter.CheckBox -> filter.state = true
            is EntryFilter.TriState -> filter.state = EntryFilter.TriState.STATE_INCLUDE
            is EntryFilter.Select<*> -> filter.state = 1
            is EntryFilter.Sort -> filter.state = EntryFilter.Sort.Selection(index = 0, ascending = true)
            is EntryFilter.PagedGroup<*> -> error("Paged groups are covered separately")
            else -> error("Unsupported interactive filter: ${filter::class.simpleName}")
        }
    }
}
