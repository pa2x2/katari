package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.filter.detachedCopy
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CatalogFilterRevisionTest {
    @Test
    fun `republishing the same mutable list still advances the revision`() {
        val state = initialCatalogState("").copy(filterRevision = 7)
        val filters = EntryFilterList(object : EntryFilter.CheckBox("English", true) {}).detachedCopy()
        (state.filters === filters) shouldBe false

        val republished = state.withRepublishedDraftFilters(filters)
        republished.filterRevision shouldBe 8
        (republished.filters === filters) shouldBe true

        // Mutating entries in place must not retroactively change the published revision.
        (republished.filters.single() as EntryFilter.CheckBox).state = false
        republished.filterRevision shouldBe 8
        val again = republished.withRepublishedDraftFilters(republished.filters)
        again.filterRevision shouldBe 9
        (again.filters === republished.filters) shouldBe true
    }
}
