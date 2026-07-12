package tachiyomi.domain.history.model

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.EntryCover
import tachiyomi.domain.library.model.LibraryItemKey

class HistoryItemTest {

    @Test
    fun `entry history key uses history entry type`() {
        val history = HistoryWithRelations(
            id = 1L,
            chapterId = 2L,
            entryId = 3L,
            entryType = EntryType.ANIME,
            title = "Test Anime",
            chapterName = "Episode 1",
            chapterNumber = 1.0,
            readAt = null,
            readDuration = 100L,
            coverData = EntryCover(
                entryId = 3L,
                sourceId = 4L,
                isFavorite = true,
                url = "https://example.com/cover.png",
                lastModified = 5L,
            ),
        )

        HistoryItem.EntryHistory(history).key shouldBe LibraryItemKey(EntryType.ANIME, 2L)
    }
}
