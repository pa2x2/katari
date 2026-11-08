package eu.kanade.tachiyomi.source.aniyomi.catalogue

import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import eu.kanade.tachiyomi.animesource.model.AnimeFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.filter.detachedCopy
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class AniyomiFilterBindingTest {

    private class YearFilter : AnimeFilter.Text("Year")

    private class GenreFilter(name: String) : AnimeFilter.CheckBox(name)

    private class GenresFilter : AnimeFilter.Group<GenreFilter>("Genres", listOf(GenreFilter("Action")))

    @Test
    fun `a search hands the extension its own filters holding the values, and leaves them as they were afterwards`() =
        runTest {
            // Extensions return the same filter objects every time and read their own subclasses in requests
            val year = YearFilter()
            val genres = GenresFilter()
            val edited = AnimeFilterList(year, genres).toEntryFilterList().detachedCopy()
            (edited[0] as EntryFilter.Text).state = "2024"
            ((edited[1] as EntryFilter.Group<*>).state[0] as EntryFilter.CheckBox).state = true

            val seen = edited.withAnimeFilterValues { filters ->
                val searchedYear = filters.filterIsInstance<YearFilter>().single()
                val searchedGenre = filters.filterIsInstance<GenresFilter>().single().state.single()
                listOf(searchedYear === year, searchedYear.state, searchedGenre.state)
            }

            seen shouldBe listOf(true, "2024", true)
            year.state shouldBe ""
            genres.state.single().state shouldBe false
        }
}
