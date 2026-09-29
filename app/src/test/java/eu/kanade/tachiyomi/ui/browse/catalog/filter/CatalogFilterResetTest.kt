package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.filter.detachedCopy
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CatalogFilterResetTest {
    @Test
    fun `failed reset retains applied results and retry reloads defaults without restoring a preset`() = runTest {
        val defaults = EntryFilterList(object : EntryFilter.CheckBox("English", true) {})
        val initial = initialCatalogState("cats").initializeForSource(defaults.detachedCopy())
        (initial.filters.single() as EntryFilter.CheckBox).state = false
        val applied = requireNotNull(initial.applyFilterDraft())
        val state = MutableStateFlow(applied.copy(repairNeedsSave = true))
        val failure = IllegalStateException("Options unavailable")
        var fail = true
        val reset = CatalogFilterReset(
            state,
            loadFilters = {
                if (fail) throw failure
                defaults.detachedCopy()
            },
            discardEdits = {},
            retainSessions = {},
        )
        reset.reset()
        state.value.filterState shouldBe FilterUiState.Error(failure)
        state.value.filterResetPending shouldBe true
        state.value.applyFilterDraft() shouldBe null
        (state.value.pageableListing === applied.listing) shouldBe true
        state.value.filters.single().state shouldBe false

        fail = false
        reset.reset()
        state.value.filterResetPending shouldBe false
        state.value.filters.single().state shouldBe true
        (state.value.pageableListing === applied.listing) shouldBe true
        (state.value.listing as CatalogScreenModel.Listing.Search).filters.single().state shouldBe false
        // Reloading defaults does not silently save a preset that still requires repair.
        state.value.repairNeedsSave shouldBe true
        state.value.applyFilterDraft() shouldBe null
    }

    @Test
    fun `reset discards queued paged edits before loading fresh filters`() = runTest {
        val filters = EntryFilterList(CatalogPagedFilterFixture()).detachedCopy()
        val state = MutableStateFlow(initialCatalogState("cats").initializeForSource(filters))
        val edits = CatalogPagedFilterEdits(this) { state.value = state.value.copy(pendingFilterEdits = it) }
        val group = filters.single() as EntryFilter.PagedGroup<*>
        edits.submit(
            group,
            EntryFilterPageItem("action", "Action"),
            object : EntryFilter.CheckBox("Action", true) {},
        ) { error("A reset must discard the pending edit") }
        val fresh = EntryFilterList(CatalogPagedFilterFixture()).detachedCopy()
        CatalogFilterReset(state, { fresh }, { edits.discard() }, {}).reset()
        runCurrent()
        state.value.pendingFilterEdits shouldBe 0
        (state.value.filters.single() as EntryFilter.PagedGroup<*>).currentSelectedItemCount() shouldBe 0
    }
}
