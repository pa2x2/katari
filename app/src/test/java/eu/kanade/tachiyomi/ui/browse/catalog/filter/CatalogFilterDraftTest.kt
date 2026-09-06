package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.domain.source.model.FeedListingMode
import eu.kanade.domain.source.model.FilterStateNode
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.EntryDateFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadata
import eu.kanade.tachiyomi.source.entry.filter.EntryPartialDate
import eu.kanade.tachiyomi.source.filter.detachedCopy
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class CatalogFilterDraftTest {
    @Test
    fun `saving and closing a draft preserves applied results until explicit apply`() {
        val state = initialCatalogState("").initializeForSource(
            EntryFilterList(object : EntryFilter.CheckBox("English", true) {}).detachedCopy(),
        )
        (state.filters.single() as EntryFilter.CheckBox).state = false
        val saved = state.toSavedPresetState(state.defaultFilters)
        (saved.filters.single() as FilterStateNode.CheckBox).state shouldBe false
        val closed = state.copy(dialog = null)
        (closed.listing as CatalogScreenModel.Listing.Search).filters.single().state shouldBe true
        closed.hasUnappliedFilterChanges shouldBe true
        val applied = requireNotNull(closed.applyFilterDraft())
        (applied.listing as CatalogScreenModel.Listing.Search).filters.single().state shouldBe false
        applied.hasUnappliedFilterChanges shouldBe false
        (closed.filters.single() as EntryFilter.CheckBox).state = true
        applied.listing.filters.single().state shouldBe false
    }

    @Test
    fun `preset mode and query load into draft and apply together`() {
        val original = initialCatalogState("popular").initializeForSource(EntryFilterList())
        val loaded = original.copy(draftMode = FeedListingMode.Search, draftQuery = "Cats", draftPresetId = "saved")
        loaded.listing shouldBe original.listing
        loaded.toSavedPresetState(loaded.defaultFilters).query shouldBe "Cats"
        val applied = requireNotNull(loaded.applyFilterDraft())
        (applied.listing as CatalogScreenModel.Listing.Search).query shouldBe "Cats"
        applied.appliedCustomPresetId shouldBe "saved"
    }

    @Test
    fun `invalid dates and an unsaved repair cannot be applied`() {
        val date = EntryDateFilter("Start", EntryFilterMetadata(id = "start"), EntryPartialDate(2024))
        val state = initialCatalogState("").initializeForSource(EntryFilterList(date).detachedCopy())
        (state.filters.single() as EntryFilter.Text).state = "2023-02-29"
        state.canSaveFilterDraft shouldBe false
        state.applyFilterDraft() shouldBe null
        (state.filters.single() as EntryFilter.Text).state = "2024-02-29"
        state.canSaveFilterDraft shouldBe true
        state.copy(repairNeedsSave = true).applyFilterDraft() shouldBe null
        state.copy(pendingFilterEdits = 1).canSaveFilterDraft shouldBe false
        state.copy(pendingFilterEdits = 1).applyFilterDraft() shouldBe null
    }
}
