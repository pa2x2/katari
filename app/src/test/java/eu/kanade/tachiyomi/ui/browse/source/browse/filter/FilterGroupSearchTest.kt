package eu.kanade.tachiyomi.ui.browse.source.browse.filter

import eu.kanade.tachiyomi.source.entry.EntryFilter
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class FilterGroupSearchTest {

    @Test
    fun `group search tolerates typos and retains source order`() {
        val filters = listOf(
            option("Action"),
            option("Romance"),
            option("Romance adventure"),
            option("Drama"),
        )

        filters.filterGroupOptions("romnce").map { it.name } shouldBe
            listOf("Romance", "Romance adventure")
    }

    private fun option(name: String): EntryFilter.CheckBox {
        return object : EntryFilter.CheckBox(name) {}
    }
}
