package tachiyomi.domain.library.model

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressSummary

class LibraryItemProgressTest {

    @Test
    fun `available summary exposes neutral progress values`() {
        val item = libraryItem(
            summary = summary(
                totalCount = 10L,
                consumedCount = 4L,
                bookmarkCount = 2L,
                hasStarted = true,
                continueTarget = EntryLibraryContinueTarget.Available(20L),
            ),
        )

        item.totalCount shouldBe 10L
        item.consumedCount shouldBe 4L
        item.unconsumedCount shouldBe 6L
        item.hasStarted shouldBe true
        item.hasBookmarks shouldBe true
        item.hasInProgress shouldBe false
        item.progressFraction shouldBe null
        item.canContinue shouldBe true
    }

    @Test
    fun `inapplicable summary does not manufacture zero progress`() {
        val item = libraryItem(summary = null)

        item.hasProgressSummary shouldBe false
        item.totalCount shouldBe null
        item.consumedCount shouldBe null
        item.unconsumedCount shouldBe null
        item.hasStarted shouldBe null
        item.hasBookmarks shouldBe null
        item.canContinue shouldBe false
    }

    private fun libraryItem(summary: EntryLibraryProgressSummary?): LibraryItem {
        val entry = Entry.create().copy(id = 1L, source = 1L, favorite = true, type = EntryType.MANGA)
        return LibraryItem(
            entry = entry,
            categories = emptyList(),
            sourceName = "Source",
            sourceLanguage = "en",
            sourceItemOrientation = EntryItemOrientation.VERTICAL,
            displaySourceId = 1L,
            sourceIds = setOf(1L),
            isLocal = false,
            isMerged = false,
            memberEntryIds = emptyList(),
            memberEntries = listOf(entry),
            progressSummary = summary
                ?.let(EntryLibraryProgressResolution::Available)
                ?: EntryLibraryProgressResolution.Inapplicable(entry.type),
            latestUpload = 0L,
            downloadCount = 0,
        )
    }

    private fun summary(
        totalCount: Long,
        consumedCount: Long,
        bookmarkCount: Long?,
        hasStarted: Boolean,
        continueTarget: EntryLibraryContinueTarget,
    ) = EntryLibraryProgressSummary(
        totalCount = totalCount,
        consumedCount = consumedCount,
        hasStarted = hasStarted,
        bookmarkCount = bookmarkCount,
        inProgressItemId = null,
        inProgressFraction = null,
        lastRead = 0L,
        continueTarget = continueTarget,
    )
}
