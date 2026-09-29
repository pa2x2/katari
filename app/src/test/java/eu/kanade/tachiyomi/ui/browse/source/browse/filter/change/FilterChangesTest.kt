package eu.kanade.tachiyomi.ui.browse.source.browse.filter.change

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.EntryFilterList
import eu.kanade.tachiyomi.source.filter.detachedCopy
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FilterChangesTest {
    @Test
    fun `defaults never count as changes and exclusions are counted apart`() {
        val defaults = EntryFilterList(
            object : EntryFilter.Select<String>("Type", arrayOf("All", "Manga")) {},
            object : EntryFilter.Group<EntryFilter<*>>(
                "Tags",
                listOf(
                    object : EntryFilter.CheckBox("Safe", true) {},
                    object : EntryFilter.TriState("Action") {},
                    object : EntryFilter.TriState("Horror") {},
                ),
            ) {},
        ).detachedCopy()
        val draft = defaults.detachedCopy()

        FilterChanges.of(draft, defaults).total shouldBe FilterChange.None

        val tags = (draft[1] as EntryFilter.Group<*>).state.filterIsInstance<EntryFilter<*>>()
        (tags[0] as EntryFilter.CheckBox).state = false
        (tags[1] as EntryFilter.TriState).state = EntryFilter.TriState.STATE_INCLUDE
        (tags[2] as EntryFilter.TriState).state = EntryFilter.TriState.STATE_EXCLUDE
        val changes = FilterChanges.of(draft, defaults)

        changes[draft[0]] shouldBe FilterChange.None
        changes[draft[1]] shouldBe FilterChange(3, excluded = 1)
        changes.total shouldBe FilterChange(3, excluded = 1)
    }
}
