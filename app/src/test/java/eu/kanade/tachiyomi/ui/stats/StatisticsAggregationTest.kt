package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsRange
import eu.kanade.presentation.more.stats.data.StatsTrendPoint
import eu.kanade.tachiyomi.source.entry.EntryType
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.domain.statistics.model.StatisticsActivityBucket
import tachiyomi.domain.statistics.model.StatisticsActivitySnapshot
import tachiyomi.domain.statistics.model.StatisticsActivityTimeline
import tachiyomi.domain.statistics.model.StatisticsCompletionBucket
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.Locale

class StatisticsAggregationTest {

    @Test
    fun `partial year buckets exclude activity outside exact window`() {
        val endDate = LocalDate.parse("2026-08-28")
        val startDate = endDate.minusYears(1L).plusDays(1L)
        val snapshot = StatisticsActivitySnapshot(
            profileId = 1L,
            trackingStartedAtEpochMillis = 1L,
            activity = listOf(StatisticsActivityBucket(EntryType.MANGA, startDate.toString(), 60_000L)),
            completions = listOf(StatisticsCompletionBucket(EntryType.MANGA, endDate.toString(), 1L)),
            topEntries = emptyList(),
            earlierActivity = emptyList(),
        )
        val expandedTimeline = StatisticsActivityTimeline(
            activity = listOf(
                StatisticsActivityBucket(EntryType.MANGA, startDate.minusDays(1L).toString(), 120_000L),
                StatisticsActivityBucket(EntryType.MANGA, startDate.toString(), 60_000L),
                StatisticsActivityBucket(EntryType.MANGA, endDate.plusDays(1L).toString(), 180_000L),
            ),
            completions = listOf(
                StatisticsCompletionBucket(EntryType.MANGA, startDate.minusDays(1L).toString(), 2L),
                StatisticsCompletionBucket(EntryType.MANGA, endDate.toString(), 1L),
                StatisticsCompletionBucket(EntryType.MANGA, endDate.plusDays(1L).toString(), 3L),
            ),
        )

        listOf(Locale.UK, Locale.US).forEach { locale ->
            val result = buildWindowActivity(
                snapshot = snapshot,
                window = StatsRange.ONE_YEAR.windowEndingOn(endDate, isLatest = true),
                types = listOf(EntryType.MANGA),
                locale = locale,
                navigationTimeline = expandedTimeline,
            )

            result.trend.sumOf(StatsTrendPoint::totalDurationMillis) shouldBe 60_000L
            result.trend.sumOf(StatsTrendPoint::completionCount) shouldBe 1L
            result.totalDurationByType shouldBe mapOf(EntryType.MANGA to 60_000L)
        }
    }

    @Test
    fun `previous window comparison requires a fully tracked, loaded window`() {
        val endDate = LocalDate.parse("2026-08-28")
        val window = StatsRange.SEVEN_DAYS.windowEndingOn(endDate, isLatest = true)
        val snapshot = StatisticsActivitySnapshot(
            profileId = 1L,
            trackingStartedAtEpochMillis = 0L,
            activity = listOf(StatisticsActivityBucket(EntryType.MANGA, "2026-08-27", 120_000L)),
            completions = emptyList(),
            topEntries = emptyList(),
            earlierActivity = emptyList(),
        )
        val navigation = StatisticsActivityTimeline(
            activity = listOf(
                StatisticsActivityBucket(EntryType.MANGA, "2026-08-14", 999_000L),
                StatisticsActivityBucket(EntryType.MANGA, "2026-08-15", 30_000L),
                StatisticsActivityBucket(EntryType.BOOK, "2026-08-21", 30_000L),
                StatisticsActivityBucket(EntryType.MANGA, "2026-08-27", 120_000L),
            ),
            completions = emptyList(),
        )
        fun build(trackingStartedAt: Long) = buildWindowActivity(
            snapshot = snapshot.copy(trackingStartedAtEpochMillis = trackingStartedAt),
            window = window,
            types = listOf(EntryType.MANGA, EntryType.BOOK),
            locale = Locale.UK,
            zoneId = ZoneOffset.UTC,
            navigationTimeline = navigation,
            navigationStartDate = LocalDate.parse("2026-08-15"),
        )

        build(trackingStartedAt = 0L).let { result ->
            result.previousWindow?.startDate shouldBe LocalDate.parse("2026-08-15")
            result.previousTotalDurationMillis shouldBe 60_000L
            result.previousTotalDurationByType shouldBe mapOf(EntryType.MANGA to 30_000L, EntryType.BOOK to 30_000L)
            result.trackedDayCount shouldBe 7
        }
        val trackedMidWindow = LocalDate.parse("2026-08-25").atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        build(trackingStartedAt = trackedMidWindow).let { result ->
            result.previousWindow shouldBe null
            result.previousTotalDurationMillis shouldBe null
            result.trackedDayCount shouldBe 4
        }
    }
}
