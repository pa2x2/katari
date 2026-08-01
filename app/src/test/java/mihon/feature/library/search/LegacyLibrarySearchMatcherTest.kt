package mihon.feature.library.search

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.source.local.LocalSource

class LegacyLibrarySearchMatcherTest {

    @Test
    fun `legacy selectors match visible ids and merged source identity`() {
        val merged = searchLibraryItem(
            entry = searchEntry(id = 7L, source = 10L),
            sourceIds = setOf(10L, 20L),
            displaySourceId = LibraryItem.MULTI_SOURCE_ID,
        )
        val local = searchLibraryItem(
            entry = searchEntry(id = 8L, source = LocalSource.ID),
            sourceIds = setOf(LocalSource.ID),
            displaySourceId = LocalSource.ID,
        )

        librarySearchMatcher("id:7").matches(merged) shouldBe true
        librarySearchMatcher("id:20").matches(merged) shouldBe false
        librarySearchMatcher("src:20").matches(merged) shouldBe true
        librarySearchMatcher("src:multi").matches(merged) shouldBe true
        librarySearchMatcher("src:local").matches(local) shouldBe true
    }

    @Test
    fun `legacy general constraints include display names and merged category names`() {
        val item = searchLibraryItem(
            entry = searchEntry(
                displayName = "Full Metal Alias",
                description = "Two heroic brothers",
            ),
            categories = listOf(4L, 9L),
        )
        val matcher = librarySearchMatcher(
            query = "Full Metal Alias, Favorites, -villain",
            categoryNamesById = mapOf(4L to "Archive", 9L to "Favorites"),
        )

        matcher.matches(item) shouldBe true
    }
}
