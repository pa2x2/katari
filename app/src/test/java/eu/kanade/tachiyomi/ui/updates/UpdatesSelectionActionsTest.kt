package eu.kanade.tachiyomi.ui.updates

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import mihon.entry.interactions.EntryConsumptionFeature
import mihon.entry.interactions.EntryConsumptionStatus
import mihon.entry.interactions.EntryDownloadState
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.updates.model.UpdateItem
import tachiyomi.domain.updates.model.UpdatesWithRelations

class UpdatesSelectionActionsTest {

    @Test
    fun `mark consumed actions use entry interaction capability`() {
        val unreadAnime = listOf(updateItem(16, EntryType.ANIME, read = false))
        val readAnime = listOf(updateItem(17, EntryType.ANIME, read = true))
        val partialAnime = listOf(updateItem(18, EntryType.ANIME, read = false, started = true))
        val partialManga = listOf(updateItem(19, EntryType.MANGA, read = false, started = true))
        val partialBook = listOf(updateItem(20, EntryType.BOOK, read = false, started = true))

        val feature = mockk<EntryConsumptionFeature> {
            every { canSetConsumed(any(), EntryConsumptionStatus(false, false), true) } returns true
            every { canSetConsumed(any(), EntryConsumptionStatus(true, false), true) } returns false
            every { canSetConsumed(any(), EntryConsumptionStatus(true, false), false) } returns true
            every { canSetConsumed(any(), EntryConsumptionStatus(false, true), false) } returns true
        }

        unreadAnime.hasConsumedAction(consumed = true, canSetConsumed = feature::canSetConsumed) shouldBe true
        readAnime.hasConsumedAction(consumed = true, canSetConsumed = feature::canSetConsumed) shouldBe false
        readAnime.hasConsumedAction(consumed = false, canSetConsumed = feature::canSetConsumed) shouldBe true
        partialAnime.hasConsumedAction(consumed = false, canSetConsumed = feature::canSetConsumed) shouldBe true
        partialManga.hasConsumedAction(consumed = false, canSetConsumed = feature::canSetConsumed) shouldBe true
        partialBook.hasConsumedAction(consumed = false, canSetConsumed = feature::canSetConsumed) shouldBe true
    }

    private fun updateItem(
        chapterId: Long,
        entryType: EntryType,
        read: Boolean = false,
        bookmark: Boolean = false,
        started: Boolean = read,
    ): UpdatesItem {
        val update = UpdatesWithRelations(
            entryId = chapterId * 10,
            entryType = entryType,
            entryTitle = "Entry $chapterId",
            chapterId = chapterId,
            chapterName = "Chapter $chapterId",
            scanlator = null,
            chapterUrl = "/chapter/$chapterId",
            read = read,
            bookmark = bookmark,
            started = started,
            progressPosition = 0,
            sourceId = 1,
            dateFetch = 0,
            coverData = EntryCover(
                entryId = chapterId * 10,
                sourceId = 1,
                isFavorite = true,
                url = null,
                lastModified = 0,
            ),
        )

        return UpdatesItem(
            update = UpdateItem.EntryUpdate(update, entryType),
            visibleEntryId = update.entryId,
            visibleEntryTitle = update.entryTitle,
            visibleCoverData = update.coverData,
            downloadStateProvider = { EntryDownloadState.NOT_DOWNLOADED },
            downloadProgressProvider = { 0 },
            selected = true,
        )
    }
}
