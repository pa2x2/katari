package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsLabelCount
import eu.kanade.tachiyomi.source.entry.EntryItemOrientation
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.service.EntryLibraryProgressResolution
import tachiyomi.domain.library.model.LibraryItem
import tachiyomi.domain.library.model.LibraryItemKey
import tachiyomi.domain.library.model.LibraryItemSource
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
    fun `a cross-source merge counts once under each source name but once among the other sources`() {
        val items = listOf(
            item(id = 1L, sources = listOf("A")),
            item(id = 2L, sources = listOf("A", "A")),
            item(id = 3L, sources = listOf("B")),
            item(id = 4L, sources = listOf("B")),
            item(id = 5L, sources = listOf("C")),
            item(id = 6L, sources = listOf("C")),
            item(id = 7L, sources = listOf("D", "E")),
        )

        val result = buildLibraryInsights(items, today, ZoneOffset.UTC)

        result.topSources shouldBe listOf(StatsLabelCount("A", 2), StatsLabelCount("B", 2), StatsLabelCount("C", 2))
        result.otherSourceCount shouldBe 2
        result.otherSourcesTitleCount shouldBe 1
    }

    private fun item(
        id: Long,
        genres: List<String> = emptyList(),
        categories: List<Long> = listOf(1L),
        sources: List<String> = listOf("Source"),
    ): LibraryItem {
        val entry = Entry.create().copy(id = id, type = EntryType.MANGA, title = "Title $id", genre = genres)
        return LibraryItem(
            entry = entry,
            categories = categories,
            sources = sources.mapIndexed { index, name -> LibraryItemSource(index.toLong(), name, "en") },
            sourceItemOrientation = EntryItemOrientation.VERTICAL,
            isLocal = false,
            isMerged = false,
            memberEntryIds = listOf(LibraryItemKey(entry.type, entry.id)),
            memberEntries = listOf(entry),
            progressSummary = EntryLibraryProgressResolution.Inapplicable(EntryType.MANGA),
            latestUpload = 0L,
            downloadCount = 0,
        )
    }
}
