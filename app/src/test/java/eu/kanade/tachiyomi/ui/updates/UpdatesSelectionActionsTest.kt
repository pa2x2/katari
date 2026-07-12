package eu.kanade.tachiyomi.ui.updates

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import mihon.entry.interactions.EntryConsumptionStatus
import mihon.entry.interactions.EntryDownloadState
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.updates.model.UpdateItem
import tachiyomi.domain.updates.model.UpdatesWithRelations

class UpdatesSelectionActionsTest {

    @Test
    fun `bookmark action is only available for manga selection`() {
        val mangaOnly = listOf(updateItem(6, EntryType.MANGA, bookmark = false))
        val animeOnly = listOf(updateItem(7, EntryType.ANIME, bookmark = false))
        val mixed = listOf(
            updateItem(8, EntryType.MANGA, bookmark = false),
            updateItem(9, EntryType.ANIME, bookmark = false),
        )

        mangaOnly.hasBookmarkAction(
            bookmark = true,
            supportsBookmark = ::supportsBookmark,
            canSetBookmarked = ::canSetBookmarked,
        ) shouldBe true
        animeOnly.hasBookmarkAction(
            bookmark = true,
            supportsBookmark = ::supportsBookmark,
            canSetBookmarked = ::canSetBookmarked,
        ) shouldBe false
        mixed.hasBookmarkAction(
            bookmark = true,
            supportsBookmark = ::supportsBookmark,
            canSetBookmarked = ::canSetBookmarked,
        ) shouldBe false
    }

    @Test
    fun `remove bookmark action is only available for fully bookmarked manga selection`() {
        val bookmarkedManga = listOf(
            updateItem(10, EntryType.MANGA, bookmark = true),
            updateItem(11, EntryType.MANGA, bookmark = true),
        )
        val partiallyBookmarkedManga = listOf(
            updateItem(12, EntryType.MANGA, bookmark = true),
            updateItem(13, EntryType.MANGA, bookmark = false),
        )
        val mixed = listOf(
            updateItem(14, EntryType.MANGA, bookmark = true),
            updateItem(15, EntryType.ANIME, bookmark = true),
        )

        bookmarkedManga.hasBookmarkAction(
            bookmark = false,
            supportsBookmark = ::supportsBookmark,
            canSetBookmarked = ::canSetBookmarked,
        ) shouldBe true
        partiallyBookmarkedManga.hasBookmarkAction(
            bookmark = false,
            supportsBookmark = ::supportsBookmark,
            canSetBookmarked = ::canSetBookmarked,
        ) shouldBe false
        mixed.hasBookmarkAction(
            bookmark = false,
            supportsBookmark = ::supportsBookmark,
            canSetBookmarked = ::canSetBookmarked,
        ) shouldBe false
    }

    @Test
    fun `mark consumed actions use entry interaction capability`() {
        val unreadAnime = listOf(updateItem(16, EntryType.ANIME, read = false))
        val readAnime = listOf(updateItem(17, EntryType.ANIME, read = true))
        val partialAnime = listOf(updateItem(18, EntryType.ANIME, read = false, lastPageRead = 4))
        val partialManga = listOf(updateItem(19, EntryType.MANGA, read = false, lastPageRead = 4))

        unreadAnime.hasConsumedAction(consumed = true, canSetConsumed = ::canSetConsumed) shouldBe true
        readAnime.hasConsumedAction(consumed = true, canSetConsumed = ::canSetConsumed) shouldBe false
        readAnime.hasConsumedAction(consumed = false, canSetConsumed = ::canSetConsumed) shouldBe true
        partialAnime.hasConsumedAction(consumed = false, canSetConsumed = ::canSetConsumed) shouldBe false
        partialManga.hasConsumedAction(consumed = false, canSetConsumed = ::canSetConsumed) shouldBe true
    }

    private fun supportsBookmark(entryType: EntryType): Boolean {
        return entryType == EntryType.MANGA
    }

    private fun canSetBookmarked(
        entryType: EntryType,
        status: EntryConsumptionStatus,
        bookmarked: Boolean,
    ): Boolean {
        return supportsBookmark(entryType) && status.bookmarked != bookmarked
    }

    private fun canSetConsumed(
        entryType: EntryType,
        status: EntryConsumptionStatus,
        consumed: Boolean,
    ): Boolean {
        return when (consumed) {
            true -> !status.consumed
            false -> status.consumed || (entryType == EntryType.MANGA && status.hasPartialProgress)
        }
    }

    private fun updateItem(
        chapterId: Long,
        entryType: EntryType,
        read: Boolean = false,
        bookmark: Boolean = false,
        lastPageRead: Long = 0,
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
            lastPageRead = lastPageRead,
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
