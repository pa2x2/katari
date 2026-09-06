package eu.kanade.tachiyomi.ui.browse.catalog

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.filter.detachedCopy
import eu.kanade.tachiyomi.source.filter.withSourceFilterValues
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CatalogPagedFilterEditsTest {
    @Test
    fun `pending edits retain the clicked value and source concrete type across in flight requests`() = runTest {
        val source = CatalogPagedFilterFixture()
        val filters = EntryFilterList(source).detachedCopy()
        val group = filters.single() as EntryFilter.PagedGroup<*>
        val item = EntryFilterPageItem("action", "Action")
        val clicked = object : EntryFilter.CheckBox("Action", true) {}
        val entered = CompletableDeferred<Unit>()
        val finish = CompletableDeferred<Unit>()
        val request = async {
            filters.withSourceFilterValues {
                entered.complete(Unit)
                finish.await()
            }
        }
        entered.await()
        var pending = 0
        val edits = CatalogPagedFilterEdits(this) { pending = it }
        edits.submit(group, item, clicked) { it shouldBe true }
        clicked.state = false
        pending shouldBe 1
        finish.complete(Unit)
        request.await()
        runCurrent()
        pending shouldBe 0
        group.encodeCurrentState() shouldBe "action"
        source.state shouldBe emptySet()
        val copied = filters.detachedCopy().single() as EntryFilter.PagedGroup<*>
        copied.resetState()
        copied.currentSelectedItemCount() shouldBe 0
    }

    @Test
    fun `discarding an edit before its coroutine starts releases the pending save and apply gate`() = runTest {
        val filters = EntryFilterList(CatalogPagedFilterFixture()).detachedCopy()
        val group = filters.single() as EntryFilter.PagedGroup<*>
        var pending = 0
        val edits = CatalogPagedFilterEdits(this) { pending = it }
        edits.submit(group, EntryFilterPageItem("action", "Action"), object : EntryFilter.CheckBox("Action", true) {}) {
            error("A discarded edit must not publish a value")
        }
        edits.discard(group)
        runCurrent()
        pending shouldBe 0
        group.currentSelectedItemCount() shouldBe 0
    }
}
