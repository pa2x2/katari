package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryGrouping
import tachiyomi.domain.library.model.LibraryGroupingDimension

class LibraryActivePageResolutionTest {

    @Test
    fun `the chosen page comes back after a filter hid it and the library fell back to another page`() {
        val grouping = LibraryGrouping(listOf(LibraryGroupingDimension.EntryType))
        val manga = page(EntryType.MANGA)
        val anime = page(EntryType.ANIME)
        val book = page(EntryType.BOOK)

        val filtered = listOf(manga, book)
        val fallbackIndex = filtered.resolveActivePageIndex(
            chosenPageId = anime.id,
            previousPage = anime,
            fallbackPageIndex = 1,
            previousGrouping = grouping,
            newGrouping = grouping,
        )
        filtered[fallbackIndex] shouldBe manga

        listOf(manga, anime, book).resolveActivePageIndex(
            chosenPageId = anime.id,
            previousPage = manga,
            fallbackPageIndex = fallbackIndex,
            previousGrouping = grouping,
            newGrouping = grouping,
        ) shouldBe 1
    }

    private fun page(type: EntryType): LibraryPage {
        val tab = LibraryPageTab(id = "type:${type.name}", title = type.name)
        return LibraryPage(id = tab.id, primaryTab = tab, entryType = type)
    }
}
