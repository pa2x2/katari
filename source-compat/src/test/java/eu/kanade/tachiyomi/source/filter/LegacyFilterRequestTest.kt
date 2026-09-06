package eu.kanade.tachiyomi.source.filter

import eu.kanade.tachiyomi.source.adapter.LegacyMangaSourceAdapter
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.model.Filter
import eu.kanade.tachiyomi.source.model.FilterList
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

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
    }

    @Test
    fun `filters appearing after capture do not block the captured search`() = runTest {
        var populated = false
        val source = LegacyFilterSourceFixture(
            filters = { if (populated) FilterList(Genres(), GenreHeader()) else FilterList(Genres()) },
            search = { _, query, filters ->
                query shouldBe "cats"
                filters.size shouldBe 1
                (filters.single() as Genres).state.single().isExcluded() shouldBe true
            },
        )
        val adapter = LegacyMangaSourceAdapter(source)
        val captured = adapter.getFilterList().detachedCopy()
        ((captured.single() as EntryFilter.Group<*>).state.single() as EntryFilter.TriState).state =
            EntryFilter.TriState.STATE_EXCLUDE
        populated = true
        captured.withSourceFilterValues { adapter.getSearchContent(1, "cats", it) }
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
        genres.state.single().isIgnored() shouldBe true
    }

    @Test
    fun `separately captured requests sharing cached legacy filters serialize and restore on failure`() = runTest {
        val choice = GenreChoice(arrayOf("Any", "Action", "Comedy"))
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val observed = mutableListOf<String>()
        val source = LegacyFilterSourceFixture(
            filters = { FilterList(choice) },
            search = { _, query, filters ->
                val genre = filters.single() as GenreChoice
                if (query == "first") {
                    entered.complete(Unit)
                    finish.await()
                    genre.values[genre.state] shouldBe "Action"
                    error("Request failed")
                }
                observed += genre.values[genre.state]
            },
        )
        val adapter = LegacyMangaSourceAdapter(source)
        val first = adapter.getFilterList().detachedCopy()
        val second = adapter.getFilterList().detachedCopy()
        (first.single() as EntryFilter.Select<*>).state = 1
        (second.single() as EntryFilter.Select<*>).state = 2
        val failed = async {
            assertThrows<IllegalStateException> { adapter.getSearchContent(1, "first", first) }
        }
        entered.await()
        val succeeding = async(start = CoroutineStart.UNDISPATCHED) { adapter.getSearchContent(1, "second", second) }
        observed shouldBe emptyList()
        finish.complete(Unit)
        failed.await()
        succeeding.await()
        observed shouldBe listOf("Comedy")
        choice.state shouldBe 0
    }

    @Test
    fun `cancelled legacy searches restore source state and allow the next request`() = runTest {
        val genres = Genres()
        val entered = CompletableDeferred<Unit>()
        val source = LegacyFilterSourceFixture(
            filters = { FilterList(genres) },
            search = { _, query, filters ->
                (filters.single() as Genres).state.single().isExcluded() shouldBe true
                if (query == "cancel") {
                    entered.complete(Unit)
                    awaitCancellation()
                }
            },
        )
        val adapter = LegacyMangaSourceAdapter(source)
        val filters = adapter.getFilterList().detachedCopy()
        ((filters.single() as EntryFilter.Group<*>).state.single() as EntryFilter.TriState).state =
            EntryFilter.TriState.STATE_EXCLUDE
        val request = launch { filters.withSourceFilterValues { adapter.getSearchContent(1, "cancel", it) } }
        entered.await()
        request.cancelAndJoin()
        genres.state.single().isIgnored() shouldBe true
        filters.withSourceFilterValues { adapter.getSearchContent(1, "retry", it) }
        genres.state.single().isIgnored() shouldBe true
    }
}
