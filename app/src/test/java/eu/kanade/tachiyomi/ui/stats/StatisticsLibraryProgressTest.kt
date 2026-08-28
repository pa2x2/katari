package eu.kanade.tachiyomi.ui.stats

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

class StatisticsLibraryProgressTest {

    @Test
    fun `partial coverage keeps supported progress and reports unavailable titles`() {
        val result = buildLibraryProgress(
            listOf(
                item(id = 1L, progress = availableProgress(consumed = 0L, hasStarted = true)),
                item(id = 2L, progress = EntryLibraryProgressResolution.Inapplicable(EntryType.BOOK)),
            ),
        )

        checkNotNull(result)
        result.notStarted shouldBe 0
        result.inProgress shouldBe 1
        result.total shouldBe 1
        result.unavailable shouldBe 1
        result.libraryTotal shouldBe 2
        result.isPartial shouldBe true
    }

    @Test
    fun `authoritative started state classifies zero-count progress`() {
        val result = buildLibraryProgress(
            listOf(
                item(id = 1L, progress = availableProgress(consumed = 0L, hasStarted = false)),
                item(id = 2L, progress = availableProgress(consumed = 0L, hasStarted = true)),
            ),
        )

        checkNotNull(result)
        result.notStarted shouldBe 1
        result.inProgress shouldBe 1
    }

    private fun item(id: Long, progress: EntryLibraryProgressResolution): LibraryItem {
        val entry = Entry.create().copy(id = id, type = EntryType.BOOK, title = "Book $id")
        return LibraryItem(
            entry = entry,
            categories = emptyList(),
            sourceName = "Source",
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

    private fun availableProgress(
        consumed: Long,
        hasStarted: Boolean,
    ): EntryLibraryProgressResolution = EntryLibraryProgressResolution.Available(
        EntryLibraryProgressSummary(
            totalCount = 10L,
            consumedCount = consumed,
            hasStarted = hasStarted,
            bookmarkCount = 0L,
            inProgressItemId = null,
            inProgressFraction = null,
            lastRead = 0L,
            continueTarget = EntryLibraryContinueTarget.NoNext,
        ),
    )
}
