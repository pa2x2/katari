package mihon.feature.library.search

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.library.model.LibraryItem

class LibrarySearchFieldMatcherTest {

    @Test
    fun `explicit fields search generic entry data and all merged source ids`() {
        val item = searchLibraryItem(
            entry = searchEntry(displayName = "Custom title", notes = "Remember this"),
            sourceIds = setOf(10L, 20L),
            displaySourceId = LibraryItem.MULTI_SOURCE_ID,
            sourceLanguage = "en",
        )
        val query = "title:\"Custom title\" && notes:\"Remember this\" && language:en && " +
            "source:\"Second source\" && source_id:20"

        librarySearchMatcher(
            query = query,
            sourceNames = { listOf("First source", "Second source") },
        ).matches(item) shouldBe true
        librarySearchMatcher("source_id:30").matches(item) shouldBe false
    }
}
