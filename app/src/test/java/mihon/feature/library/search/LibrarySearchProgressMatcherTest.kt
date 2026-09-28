package mihon.feature.library.search

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution

class LibrarySearchProgressMatcherTest {

    @Test
    fun `progress comparisons use available summaries and never manufacture inapplicable values`() {
        val available = searchLibraryItem(
            entry = searchEntry(type = EntryType.BOOK),
            progressSummary = availableSearchProgress(total = 10L, consumed = 4L),
        )
        val inapplicable = searchLibraryItem(
            entry = searchEntry(type = EntryType.ANIME),
            progressSummary = EntryLibraryProgressResolution.Inapplicable(EntryType.ANIME),
        )

        librarySearchMatcher("unread=6 && read>=4 && total=10").matches(available) shouldBe true
        librarySearchMatcher("unread=0").matches(inapplicable) shouldBe false
        librarySearchMatcher("-unread=0").matches(inapplicable) shouldBe true
    }
}
