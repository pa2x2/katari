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

    private fun item(id: Long, genres: List<String>, categories: List<Long>): LibraryItem {
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
            progressSummary = EntryLibraryProgressResolution.Inapplicable(EntryType.MANGA),
            latestUpload = 0L,
            downloadCount = 0,
        )
    }
}
