package eu.kanade.tachiyomi.ui.browse.source.browse

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterPage
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageRequest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SourceFilterRouteTest {
    @Test
    fun `live resolution descends into nested groups and rejects stale paths`() {
        val nested = PagedFixture("Tags")
        val filters = EntryFilterList(
            EntryFilterList(
                object : EntryFilter.CheckBox("Manga", true) {},
                nested,
            ).let { object : EntryFilter.Group<EntryFilter<*>>("Categories", it) {} },
        )
        val path = requireNotNull(filters.pathTo(nested))
        path shouldBe listOf(0, 1)
        (filters.resolvePagedGroup(path) === nested) shouldBe true

        filters.resolvePagedGroup(listOf(5)) shouldBe null
        filters.resolvePagedGroup(listOf(0, 5)) shouldBe null
        filters.resolvePagedGroup(emptyList()) shouldBe null
        filters.resolvePagedGroup(listOf(0)) shouldBe null
    }

    private class PagedFixture(
        name: String,
        selected: Set<String> = emptySet(),
    ) : EntryFilter.PagedGroup<Set<String>>(name, selected) {
        override suspend fun getPage(request: EntryFilterPageRequest) =
            EntryFilterPage(listOf(EntryFilterPageItem("a", "A")))

        override fun projectItem(item: EntryFilterPageItem, previous: EntryFilter<*>?) =
            object : EntryFilter.CheckBox(item.label, item.id in state) {}

        override fun reduceItemUpdate(item: EntryFilterPageItem, updatedFilter: EntryFilter<*>): Set<String> =
            if ((updatedFilter as EntryFilter.CheckBox).state) state + item.id else state - item.id

        override fun selectedItemCount(state: Set<String>) = state.size

        override fun encodeState(state: Set<String>) = state.sorted().joinToString(",")

        override fun decodeState(value: String) = value.split(",").filter { it.isNotEmpty() }.toSet()
    }
}
