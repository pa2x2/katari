package mihon.feature.library.search

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution

class LibrarySearchProgressMatcherTest {

    @Test
    fun `progress comparisons use available summaries for every entry type`() {
        EntryType.entries.forEach { type ->
            val item = searchLibraryItem(
                entry = searchEntry(type = type),
                progressSummary = availableSearchProgress(total = 10L, consumed = 4L),
            )

            librarySearchMatcher("unread=6 && read>=4 && total=10").matches(item) shouldBe true
        }
    }

    @Test
    fun `progress comparisons do not manufacture values for inapplicable entry types`() {
        EntryType.entries.forEach { type ->
            val item = searchLibraryItem(
                entry = searchEntry(type = type),
                progressSummary = EntryLibraryProgressResolution.Inapplicable(type),
            )

            librarySearchMatcher("unread=0").matches(item) shouldBe false
            librarySearchMatcher("-unread=0").matches(item) shouldBe true
        }
    }
}
