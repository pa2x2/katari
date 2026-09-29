package mihon.feature.library.search

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressSummary
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey

class LibrarySearchProgressMatcherTest {

    @Test
    fun `progress comparisons use available summaries and never manufacture inapplicable values`() {
        val available = libraryItem(
            type = EntryType.BOOK,
            progress = EntryLibraryProgressResolution.Available(
                EntryLibraryProgressSummary(
                    totalCount = 10L,
                    consumedCount = 4L,
                    hasStarted = true,
                    bookmarkCount = 0L,
                    inProgressItemId = null,
                    inProgressFraction = null,
                    lastRead = 0L,
                    continueTarget = EntryLibraryContinueTarget.NoNext,
                ),
            ),
        )
        val inapplicable = libraryItem(
            type = EntryType.ANIME,
            progress = EntryLibraryProgressResolution.Inapplicable(EntryType.ANIME),
        )

        LibrarySearchMatcher("unread=6 && read>=4 && total=10", emptyMap()).matches(available) shouldBe true
        LibrarySearchMatcher("unread=0", emptyMap()).matches(inapplicable) shouldBe false
        LibrarySearchMatcher("-unread=0", emptyMap()).matches(inapplicable) shouldBe true
    }

    private fun libraryItem(type: EntryType, progress: EntryLibraryProgressResolution): LibraryItem {
        val entry = Entry.create().copy(id = 1L, source = 10L, type = type, profileId = 42L)
        return LibraryItem(
            entry = entry,
            categories = emptyList(),
            sourceName = "Test source",
            sourceLanguage = "en",
            sourceItemOrientation = EntryItemOrientation.VERTICAL,
            displaySourceId = entry.source,
            sourceIds = setOf(entry.source),
            isLocal = false,
            isMerged = false,
            memberEntryIds = listOf(LibraryItemKey(entry.type, entry.id)),
            memberEntries = listOf(entry),
            progressSummary = progress,
            latestUpload = 0L,
            downloadCount = 0,
        )
    }
}
