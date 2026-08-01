package eu.kanade.domain.source.model

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.EntryFilterPage
import eu.kanade.tachiyomi.source.entry.EntryFilterPageItem
import eu.kanade.tachiyomi.source.entry.EntryFilterPageRequest
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class PagedFilterPresetTest {
    @Test
    fun `paged group presets persist only encoded durable state`() {
        val original = filter("selected")
        val snapshot = EntryFilterList(original).snapshot()
        val restored = filter("default")

        EntryFilterList(restored).applySnapshot(snapshot)

        snapshot shouldBe listOf(FilterStateNode.PagedGroup("Options", "selected"))
        restored.state shouldBe "selected"
    }

    @Test
    fun `invalid paged group preset retains source default`() {
        val restored = filter("default", reject = "invalid")

        EntryFilterList(restored).applySnapshot(listOf(FilterStateNode.PagedGroup("Options", "invalid")))

        restored.state shouldBe "default"
    }

    private fun filter(
        initialState: String,
        reject: String? = null,
    ) = object : EntryFilter.PagedGroup<String>("Options", initialState) {
        override suspend fun getPage(request: EntryFilterPageRequest): EntryFilterPage = EntryFilterPage(emptyList())

        override fun projectItem(item: EntryFilterPageItem, previous: EntryFilter<*>?): EntryFilter<*> =
            object : EntryFilter.CheckBox(item.label) {}

        override fun reduceItemUpdate(item: EntryFilterPageItem, updatedFilter: EntryFilter<*>): String = state

        override fun selectedItemCount(state: String): Int = state.length

        override fun encodeState(state: String): String = state

        override fun decodeState(value: String): String? = value.takeUnless { it == reject }
    }
}
