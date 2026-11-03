package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadata
import eu.kanade.tachiyomi.source.entry.filter.EntryFilterMetadataProvider
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FilterSelectionStateTest {
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
