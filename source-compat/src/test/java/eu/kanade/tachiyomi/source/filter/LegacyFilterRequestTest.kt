package eu.kanade.tachiyomi.source.filter

import eu.kanade.tachiyomi.source.adapter.LegacyMangaSourceAdapter
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class LegacyFilterRequestTest {
    private class Genre : Filter.TriState("Action")
    private class Genres : Filter.Group<Genre>("Genres", listOf(Genre()))
    private class GenreChoice(values: Array<String>) : Filter.Select<String>("Genre", values)
    private class GenreHeader : Filter.Header("Genres")

    @Test
    fun `applied searches keep the captured options after a later filter load reorders them`() = runTest {
        var options = arrayOf("Any", "Action", "Comedy")
        val source = LegacyFilterSourceFixture(
            filters = { FilterList(GenreChoice(options)) },
            search = { _, _, filters ->
                val genre = filters.single() as GenreChoice
                genre.values[genre.state] shouldBe "Action"
            },
        )
        val adapter = LegacyMangaSourceAdapter(source)
        val binding = EntryFilterBinding()
        val draft = binding.captureFilters(adapter::getFilterList)
        (draft.single() as EntryFilter.Select<*>).state = 1
        val applied = draft.detachedCopy()
        (draft.single() as EntryFilter.Select<*>).state = 0
        options = arrayOf("Any", "Comedy", "Action")
        binding.captureFilters(adapter::getFilterList)
        for (page in 1..2) {
            applied.withSourceFilterValues { adapter.getSearchContent(page, "", it) }
        }
        source.searchCount shouldBe 2
    }

    @Test
    fun `text searches with captured or explicit empty filters stay empty after options load`() = runTest {
        var populated = false
        val source = LegacyFilterSourceFixture(
            filters = { if (populated) FilterList(Genres()) else FilterList() },
            search = { _, query, filters ->
                query shouldBe "cats"
                filters.isEmpty() shouldBe true
            },
        )
        val adapter = LegacyMangaSourceAdapter(source)
        val captured = adapter.getFilterList().detachedCopy()
        populated = true
        for (filters in listOf(captured, EntryFilterList())) {
            filters.withSourceFilterValues { adapter.getSearchContent(1, "cats", it) }
        }
        source.searchCount shouldBe 2
    }

    @Test
    fun `legacy requests preserve concrete headers groups and nested values then restore defaults`() = runTest {
        val header = GenreHeader()
        val genres = Genres()
        val source = LegacyFilterSourceFixture(
            filters = { FilterList(header, genres) },
            search = { _, _, filters ->
                (filters[0] === header) shouldBe true
                (filters[1] === genres) shouldBe true
                (filters[1] as Genres).state.single().isExcluded() shouldBe true
            },
        )
        val adapter = LegacyMangaSourceAdapter(source)
        val draft = adapter.getFilterList().detachedCopy()
        ((draft[1] as EntryFilter.Group<*>).state.single() as EntryFilter.TriState).state =
            EntryFilter.TriState.STATE_EXCLUDE
        draft.withSourceFilterValues { adapter.getSearchContent(1, "", it) }
        source.searchCount shouldBe 1
        genres.state.single().isIgnored() shouldBe true
    }
}
