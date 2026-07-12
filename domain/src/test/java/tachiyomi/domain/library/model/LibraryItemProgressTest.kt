package tachiyomi.domain.library.model

import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry

class LibraryItemProgressTest {

    @Test
    fun `convenience properties expose neutral progress counts`() {
        val item = libraryItem(
            progress = ProgressState(
                totalCount = 10L,
                consumedCount = 4L,
                bookmarkCount = 2L,
                hasStarted = true,
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
    fun `target based progress can continue only when a target exists`() {
        val item = libraryItem(
            progress = ProgressState(
                totalCount = 10L,
                consumedCount = 10L,
                hasStarted = true,
                inProgressItemId = 20L,
                inProgressFraction = 0.5f,
                continueMode = ProgressState.ContinueMode.TARGET_AVAILABLE,
            ),
            continueEntryId = 20L,
        )

        item.unconsumedCount shouldBe 0L
        item.hasInProgress shouldBe true
        item.progressFraction shouldBe 0.5f
        item.canContinue shouldBe true

        item.copy(continueEntryId = null).canContinue shouldBe false
    }

    private fun libraryItem(
        progress: ProgressState,
        continueEntryId: Long? = null,
    ): LibraryItem {
        val entry = Entry.create().copy(
            id = 1L,
            source = 1L,
            favorite = true,
            type = EntryType.MANGA,
        )

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
            progress = progress,
            latestUpload = 0L,
            lastRead = 0L,
            continueEntryId = continueEntryId,
            downloadCount = 0,
        )
    }
}
