package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.domain.source.model.FilterStateNode
import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.filter.detachedCopy
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CatalogFilterResetTest {
    @Test
    fun `reset loads newly available options and defaults without changing the applied search`() = runTest {
        val initial = EntryFilterList(object : EntryFilter.Select<String>("Genre", arrayOf("Any")) {})
        val state = MutableStateFlow(
            initialCatalogState("cats").initializeForSource(initial.detachedCopy()).copy(
                draftPresetId = "saved",
                appliedCustomPresetId = "saved",
            ),
        )
        val applied = state.value.listing
        val loaded = CompletableDeferred<EntryFilterList>()
        val fresh = EntryFilterList(
            object : EntryFilter.Select<String>("Genre", arrayOf("Any", "Action"), 1) {},
        ).detachedCopy()
        var retained: EntryFilterList? = null
        val reset = CatalogFilterReset(state, { loaded.await() }, {}, { retained = it })
        val request = async { reset.reset() }
        runCurrent()
        state.value.filterState shouldBe FilterUiState.Loading
        (state.value.pageableListing === applied) shouldBe true
        state.value.applyFilterDraft() shouldBe null
        loaded.complete(fresh)
        request.await()

        (retained === fresh) shouldBe true
        state.value.filterState shouldBe FilterUiState.Ready
        val genre = state.value.filters.single() as EntryFilter.Select<*>
        genre.values.toList() shouldBe listOf("Any", "Action")
        genre.state shouldBe 1
        genre.state = 0
        state.value.defaultFilters.single().state shouldBe 1
        (state.value.pageableListing === applied) shouldBe true
        state.value.draftPresetId shouldBe "saved"
        state.value.appliedCustomPresetId shouldBe "saved"
        val updated = requireNotNull(state.value.applyFilterDraft())
        (updated.listing as CatalogScreenModel.Listing.Search).filters.single().state shouldBe 0
        updated.listing.query shouldBe "cats"
    }

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
    fun `resetting an incompatible initial preset cannot start its unapplied search`() = runTest {
        val state = MutableStateFlow(
            initialCatalogState("cats").initializeForSource(
                EntryFilterList(),
                listOf(FilterStateNode.CheckBox("English", false)),
            ),
        )
        state.value.pageableListing shouldBe null
        val fresh = EntryFilterList(object : EntryFilter.CheckBox("English", true) {}).detachedCopy()
        CatalogFilterReset(state, { fresh }, {}, {}).reset()
        state.value.repairIssues shouldBe emptyList()
        state.value.pageableListing shouldBe null
        state.value.applyFilterDraft() shouldBe null
        val repaired = state.value.copy(repairNeedsSave = false)
        val applied = requireNotNull(repaired.applyFilterDraft())
        (applied.pageableListing as CatalogScreenModel.Listing.Search).filters.single().state shouldBe true
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
