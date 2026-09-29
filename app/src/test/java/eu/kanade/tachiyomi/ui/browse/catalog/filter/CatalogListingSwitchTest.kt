package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.filter.detachedCopy
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeSameInstanceAs
import org.junit.jupiter.api.Test

class CatalogListingSwitchTest {
    private fun appliedSearch(): CatalogScreenModel.State {
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

        val listing = searched.listing as CatalogScreenModel.Listing.Search
        listing.query shouldBe "one"
        listing.filters.single().state shouldBe true
        searched.hasUnappliedFilterChanges shouldBe true
        (searched.filters.single() as EntryFilter.CheckBox).state shouldBe false
    }

    @Test
    fun `switching to popular parks the applied search and leaves the draft untouched`() {
        val applied = appliedSearch()
        val search = applied.listing

        val popular = applied.switchListing(CatalogScreenModel.Listing.Popular)

        popular.restorableSearch shouldBeSameInstanceAs search
        (popular.filters.single() as EntryFilter.CheckBox).state shouldBe true
        val restored = popular.switchListing(popular.restorableSearch!!)
        restored.listing shouldBeSameInstanceAs search
        restored.restorableSearch shouldBe null
    }
}
