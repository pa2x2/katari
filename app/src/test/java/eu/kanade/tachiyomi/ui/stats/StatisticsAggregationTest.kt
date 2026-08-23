package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.statistics.model.StatisticsActivityBucket
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsCompletionBucket
import java.time.LocalDate
import java.util.Locale

class StatisticsAggregationTest {

    @Test
    fun `overview trend fills empty days and retains each type contribution`() {
        val result = buildActivity(
            snapshot = StatisticsActivitySnapshot(
                profileId = 1L,
                trackingStartedAtEpochMillis = 1L,
                activity = listOf(
                    StatisticsActivityBucket(EntryType.MANGA, "2026-08-23", 60_000L),
                    StatisticsActivityBucket(EntryType.ANIME, "2026-08-22", 30_000L),
                ),
                completions = listOf(
                    StatisticsCompletionBucket(EntryType.ANIME, "2026-08-22", 1L),
                ),
                topEntries = emptyList(),
            ),
            range = StatsRange.SEVEN_DAYS,
            types = listOf(EntryType.MANGA, EntryType.ANIME, EntryType.BOOK),
            today = LocalDate.parse("2026-08-23"),
            locale = Locale.US,
        )

        result.trend.size shouldBe 7
        result.trend.last().durationByType shouldBe mapOf(
            EntryType.MANGA to 60_000L,
            EntryType.ANIME to 0L,
            EntryType.BOOK to 0L,
        )
        result.totalDurationMillis shouldBe 90_000L
        result.currentStreakDays shouldBe 2
        result.currentStreakDaysByType[EntryType.MANGA] shouldBe 1
        result.currentStreakDaysByType[EntryType.ANIME] shouldBe 0
    }
}
