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

class StatisticsLibraryInsightsTest {

    @Test
    fun `library insights normalize genres and count distinct categories`() {
        val first = item(
            id = 1L,
            genres = listOf("Science   Fiction", "Drama"),
            categories = listOf(1L, 2L),
        )
        val second = item(
            id = 2L,
            genres = listOf(" science fiction "),
            categories = listOf(2L),
        )

        val result = buildLibraryInsights(listOf(first, second))

        result.topGenre shouldBe "Science Fiction"
        result.categoryCount shouldBe 2
    }

    private fun item(
        id: Long,
        genres: List<String>,
        categories: List<Long>,
    ): LibraryItem {
        val entry = Entry.create().copy(id = id, type = EntryType.MANGA, title = "Title $id", genre = genres)
        return LibraryItem(
            entry = entry,
            categories = categories,
            sourceName = "Source",
            sourceLanguage = "en",
            sourceItemOrientation = EntryItemOrientation.VERTICAL,
            displaySourceId = entry.source,
            sourceIds = setOf(entry.source),
            isLocal = false,
            isMerged = false,
            memberEntryIds = listOf(LibraryItemKey(entry.type, entry.id)),
            memberEntries = listOf(entry),
            progressSummary = EntryLibraryProgressResolution.Available(
                EntryLibraryProgressSummary(
                    totalCount = 1L,
                    consumedCount = 0L,
                    hasStarted = false,
                    bookmarkCount = 0L,
                    inProgressItemId = null,
                    inProgressFraction = null,
                    lastRead = 0L,
                    continueTarget = EntryLibraryContinueTarget.NoNext,
                ),
            ),
            latestUpload = 0L,
            downloadCount = 0,
        )
    }
}
