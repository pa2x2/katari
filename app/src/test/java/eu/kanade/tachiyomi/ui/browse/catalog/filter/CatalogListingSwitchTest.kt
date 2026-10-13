package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.filter.detachedCopy
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CatalogListingSwitchTest {
    private fun appliedSearch(): CatalogViewModel.State {
        val state = initialCatalogState("").initializeForSource(
            EntryFilterList(object : EntryFilter.CheckBox("Completed") {}).detachedCopy(),
        )
        (state.filters.single() as EntryFilter.CheckBox).state = true
        return requireNotNull(state.applyFilterDraft())
    }

    @Test
    fun `toolbar search keeps the applied filters and never sends the unapplied draft`() {
        val applied = appliedSearch()
        (applied.filters.single() as EntryFilter.CheckBox).state = false

        val searched = applied.searchWithAppliedFilters("one")

        val listing = searched.listing as CatalogViewModel.Listing.Search
        listing.query shouldBe "one"
        listing.filters.single().state shouldBe true
        searched.hasUnappliedFilterChanges shouldBe true
        (searched.filters.single() as EntryFilter.CheckBox).state shouldBe false
    }
}
