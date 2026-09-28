package eu.kanade.tachiyomi.ui.history.activity

import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.statistics.model.StatisticsActivityBucket
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsCompletionBucket
import tachiyomi.domain.statistics.model.StatisticsSessionSummary
import java.time.LocalDate

class HistoryActivitySummaryTest {

    @Test
    fun `type screen only counts that type and shows every day of a short range`() {
        val summary = summarizeHistoryActivity(
            snapshot = snapshot(
                StatisticsActivityBucket(EntryType.MANGA, "2026-09-01", 60_000L),
                StatisticsActivityBucket(EntryType.ANIME, "2026-09-01", 90_000L),
                StatisticsActivityBucket(EntryType.MANGA, "2026-09-03", 30_000L),
            ),
            type = EntryType.MANGA,
            startDate = LocalDate.parse("2026-09-01"),
            endDate = LocalDate.parse("2026-09-03"),
        )

        summary.totalDurationMillis shouldBe 90_000L
        summary.sessionCount shouldBe 2L
        summary.completionCount shouldBe 1L
        summary.hourlyDurationMillis shouldBe null
        summary.buckets.map { it.durationMillis } shouldBe listOf(60_000L, 0L, 30_000L)
    }

    private fun snapshot(vararg activity: StatisticsActivityBucket) = StatisticsActivitySnapshot(
        profileId = 1L,
        trackingStartedAtEpochMillis = 1L,
        activity = activity.toList(),
        completions = listOf(
            StatisticsCompletionBucket(EntryType.MANGA, "2026-09-01", 1L),
            StatisticsCompletionBucket(EntryType.ANIME, "2026-09-01", 3L),
        ),
        topEntries = emptyList(),
        earlierActivity = emptyList(),
        sessions = listOf(
            StatisticsSessionSummary(
                EntryType.MANGA,
                sessionCount = 2L,
                averageDurationMillis = 1L,
                longestDurationMillis = 1L,
            ),
            StatisticsSessionSummary(
                EntryType.ANIME,
                sessionCount = 4L,
                averageDurationMillis = 1L,
                longestDurationMillis = 1L,
            ),
        ),
    )
}
