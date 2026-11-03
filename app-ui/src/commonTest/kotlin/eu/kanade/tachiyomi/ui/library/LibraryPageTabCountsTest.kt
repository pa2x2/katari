package eu.kanade.tachiyomi.ui.library

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryItemKey

class LibraryPageTabCountsTest {

    @Test
    fun `tab counts deduplicate descendants within their scoped paths`() {
        val manga = LibraryPageTab(id = "type:MANGA", title = "Manga")
        val anime = LibraryPageTab(id = "type:ANIME", title = "Anime")
        val empty = LibraryPageTab(id = "category:empty", title = "Empty")
        val sharedSource = LibraryPageTab(id = "source:10", title = "Shared source")
        val pages = listOf(
            page(
                id = "type:MANGA/source:10/category:1",
                primary = manga,
                secondary = sharedSource,
                tertiary = LibraryPageTab("category:1", "First"),
                itemIds = listOf(key(1L), key(2L)),
            ),
            page(
                id = "type:MANGA/source:10/category:2",
                primary = manga,
                secondary = sharedSource,
                tertiary = LibraryPageTab("category:2", "Second"),
                itemIds = listOf(key(1L), key(3L)),
            ),
            page(
                id = "type:ANIME/source:10/category:1",
                primary = anime,
                secondary = sharedSource,
                tertiary = LibraryPageTab("category:1", "First"),
                itemIds = listOf(LibraryItemKey(EntryType.ANIME, 1L)),
            ),
            page(id = "category:empty", primary = empty),
        )

        val counted = pages.withTabItemCounts()

        counted.map { it.primaryTab.itemCount } shouldContainExactly listOf(3, 3, 1, 0)
        counted.map { it.secondaryTab?.itemCount } shouldContainExactly listOf(3, 3, 1, null)
        counted.map { it.tertiaryTab?.itemCount } shouldContainExactly listOf(2, 2, 1, null)
        pages.flatMap { listOfNotNull(it.primaryTab, it.secondaryTab, it.tertiaryTab) }
            .all { it.itemCount == null } shouldBe true
    }

    private fun page(
        id: String,
        primary: LibraryPageTab,
        secondary: LibraryPageTab? = null,
        tertiary: LibraryPageTab? = null,
        itemIds: List<LibraryItemKey> = emptyList(),
    ) = LibraryPage(
        id = id,
        primaryTab = primary,
        secondaryTab = secondary,
        tertiaryTab = tertiary,
        itemIds = itemIds,
    )

    private fun key(id: Long) = LibraryItemKey(EntryType.MANGA, id)
}
