package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.filter.detachedCopy
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CatalogAppliedFilterRemovalTest {
    private fun EntryFilterList.tags() =
        (single() as EntryFilter.Group<*>).state.map { (it as EntryFilter.TriState).state }

    private fun appliedActionAndHorror(): CatalogScreenModel.State {
        val initial = initialCatalogState("").initializeForSource(
            EntryFilterList(
                object : EntryFilter.Group<EntryFilter<*>>(
                    "Tags",
                    listOf(object : EntryFilter.TriState("Action") {}, object : EntryFilter.TriState("Horror") {}),
                ) {},
            ).detachedCopy(),
        )
        (initial.filters.single() as EntryFilter.Group<*>).state.forEach {
            (it as EntryFilter.TriState).state = EntryFilter.TriState.STATE_INCLUDE
        }
        return requireNotNull(initial.applyFilterDraft())
    }

    @Test
    fun `removing an applied value resets only that value and never applies unapplied edits`() {
        val inSync = appliedActionAndHorror().withoutAppliedFilter(listOf(0, 0))
        inSync.listing.filters.tags() shouldBe listOf(0, 1)
        inSync.filters.tags() shouldBe listOf(0, 1)

        val edited = appliedActionAndHorror()
        ((edited.filters.single() as EntryFilter.Group<*>).state[1] as EntryFilter.TriState).state =
            EntryFilter.TriState.STATE_EXCLUDE
        val diverged = edited.withoutAppliedFilter(listOf(0, 0))
        diverged.listing.filters.tags() shouldBe listOf(0, 1)
        diverged.filters.tags() shouldBe listOf(1, 2)
    }
}
