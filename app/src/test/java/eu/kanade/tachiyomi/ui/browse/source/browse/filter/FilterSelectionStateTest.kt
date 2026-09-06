package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadata
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadataProvider
import eu.kanade.tachiyomi.source.filter.detachedCopy
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FilterSelectionStateTest {
    @Test
    fun `default constraints and exclusions count while clearing and resetting differ`() {
        val english = object : EntryFilter.CheckBox("English", true) {}
        val genre = object : EntryFilter.TriState("Horror", EntryFilter.TriState.STATE_EXCLUDE) {}
        val group = object : EntryFilter.Group<EntryFilter<*>>("Settings", listOf(english, genre)) {}
        val defaults = EntryFilterList(group).detachedCopy()
        group.activeCount() shouldBe 2
        group.clearSelection()
        group.activeCount() shouldBe 0
        resetFilterToDefault(group, EntryFilterList(group), defaults)
        english.state shouldBe true
        genre.state shouldBe EntryFilter.TriState.STATE_EXCLUDE
        group.activeCount() shouldBe 2
    }

    @Test
    fun `legacy dropdown neutrality remains unknown unless declared by the source`() {
        val unknown = object : EntryFilter.Select<String>("Language", arrayOf("English", "All")) {}
        unknown.activeCount() shouldBe null
        unknown.canClear() shouldBe false
        val declared = object :
            EntryFilter.Select<String>("Language", arrayOf("English", "All")),
            EntryFilterMetadataProvider {
            override val filterMetadata = EntryFilterMetadata(
                id = "language",
                optionIds = listOf("en", "any"),
                neutralOptionId = "any",
            )
        }
        declared.activeCount() shouldBe 1
        declared.clearSelection()
        declared.state shouldBe 1
        declared.activeCount() shouldBe 0
        val sort = object : EntryFilter.Sort("Sort", arrayOf("Title"), EntryFilter.Sort.Selection(0, true)) {}
        sort.activeCount() shouldBe 0
        sort.canClear() shouldBe false
    }
}
