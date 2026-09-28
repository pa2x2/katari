package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsLabelCount
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryStatus
import tachiyomi.domain.entry.service.EntryLibraryContinueTarget
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.entry.service.EntryLibraryProgressSummary
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import java.time.LocalDate
import java.time.ZoneOffset

class StatisticsLibraryInsightsTest {

    private val today = LocalDate.parse("2026-09-28")

    @Test
    fun `genres are normalized and counted once per title`() {
        val first =
            item(id = 1L, genres = listOf("Science   Fiction", "Drama", "science fiction"), categories = listOf(1L, 2L))
        val second = item(id = 2L, genres = listOf(" science fiction "), categories = listOf(2L))

        val result = buildLibraryInsights(listOf(first, second), today, ZoneOffset.UTC)

        result.topGenres shouldBe listOf(StatsLabelCount("Science Fiction", 2), StatsLabelCount("Drama", 1))
        result.categoryCount shouldBe 2
    }

    @Test
    fun `backlog counts only known progress and additions group by month of the current year`() {
        val items = listOf(
            item(
                id = 1L,
                unconsumed = 12L,
                added = "2026-09-03",
                source = "A",
                status = EntryStatus.ONGOING,
                downloads = 4,
            ),
            item(id = 2L, unconsumed = 0L, added = "2026-01-15", source = "A", status = EntryStatus.COMPLETED),
            item(id = 3L, unconsumed = null, added = "2025-12-31", source = "B", status = EntryStatus.ONGOING),
            item(id = 4L, unconsumed = 3L, added = "2026-09-20", source = "C"),
            item(id = 5L, unconsumed = 1L, added = "2026-02-01", source = "D"),
        )

        val result = buildLibraryInsights(items, today, ZoneOffset.UTC)

        result.unconsumedCount shouldBe 16L
        result.titlesWithUnconsumed shouldBe 3
        result.downloadedCount shouldBe 4L
        result.addedByMonth shouldBe listOf(1, 1, 0, 0, 0, 0, 0, 0, 2)
        result.statusCounts[EntryStatus.ONGOING] shouldBe 2
        result.topSources.first() shouldBe StatsLabelCount("A", 2)
        result.otherSourcesTitleCount shouldBe 1
        result.otherSourceCount shouldBe 1
    }

    private fun item(
        id: Long,
        genres: List<String> = emptyList(),
        categories: List<Long> = emptyList(),
        unconsumed: Long? = 0L,
        added: String = "2026-01-01",
        source: String = "Source",
        status: EntryStatus = EntryStatus.UNKNOWN,
        downloads: Int = 0,
    ): LibraryItem {
        val entry = Entry.create().copy(
            id = id,
            type = EntryType.MANGA,
            title = "Title $id",
            genre = genres,
            status = status,
            dateAdded = LocalDate.parse(added).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        return LibraryItem(
            entry = entry,
            categories = categories,
            sourceName = source,
            sourceLanguage = "en",
            sourceItemOrientation = EntryItemOrientation.VERTICAL,
            displaySourceId = entry.source,
            sourceIds = setOf(entry.source),
            isLocal = false,
            isMerged = false,
            memberEntryIds = listOf(LibraryItemKey(entry.type, entry.id)),
            memberEntries = listOf(entry),
            progressSummary = if (unconsumed == null) {
                EntryLibraryProgressResolution.Inapplicable(EntryType.MANGA)
            } else {
                EntryLibraryProgressResolution.Available(
                    EntryLibraryProgressSummary(
                        totalCount = 20L,
                        consumedCount = 20L - unconsumed,
                        hasStarted = unconsumed < 20L,
                        bookmarkCount = 0L,
                        inProgressItemId = null,
                        inProgressFraction = null,
                        lastRead = 0L,
                        continueTarget = EntryLibraryContinueTarget.NoNext,
                    ),
                )
            },
            latestUpload = 0L,
            downloadCount = downloads,
        )
    }
}
