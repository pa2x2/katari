package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.ActivityState
import eu.kanade.presentation.more.stats.data.StatsActivity
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsRange
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.time.LocalDate

class StatisticsActivityRequestStateTest {

    @Test
    fun `failed replacement keeps displayed range and attempted target`() {
        val displayed = window(StatsRange.SEVEN_DAYS, "2026-08-23")
        val attempted = window(StatsRange.THIRTY_DAYS, "2026-08-23")
        val available = ActivityState.Available(activity(displayed), loadingTarget = attempted)

        val failed = reduceStatisticsActivityRequest(
            previous = available,
            event = StatisticsActivityLoadEvent.Failed(StatisticsActivityLoadRequest(attempted, 0L)),
        ) as ActivityState.Available

        failed.displayedRange shouldBe StatsRange.SEVEN_DAYS
        failed.data.window shouldBe displayed
        failed.failedTarget shouldBe attempted
        failed.loadingTarget shouldBe null
    }

    @Test
    fun `silent rollback refresh preserves failed target for retry`() {
        val displayed = window(StatsRange.SEVEN_DAYS, "2026-08-23")
        val attempted = window(StatsRange.THIRTY_DAYS, "2026-08-23")
        val failed = ActivityState.Available(activity(displayed), failedTarget = attempted)
        val rollbackRequest = StatisticsActivityLoadRequest(displayed, 0L)

        val loading = reduceStatisticsActivityRequest(
            previous = failed,
            event = StatisticsActivityLoadEvent.Loading(rollbackRequest),
        )
        val loaded = reduceStatisticsActivityRequest(
            previous = loading,
            event = StatisticsActivityLoadEvent.Loaded(rollbackRequest, activity(displayed)),
        ) as ActivityState.Available

        loaded.data.window shouldBe displayed
        loaded.failedTarget shouldBe attempted
        loaded.loadingTarget shouldBe null
    }

    private fun window(range: StatsRange, endDate: String): StatsActivityWindow {
        val end = LocalDate.parse(endDate)
        return range.windowEndingOn(end, isLatest = true)
    }

    private fun activity(window: StatsActivityWindow): StatsActivity = StatsActivity(
        window = window,
        totalDurationMillis = 0L,
        totalDurationByType = emptyMap(),
        currentStreakDays = 0,
        currentStreakDaysByType = emptyMap(),
        completionCount = 0L,
        completionCountByType = emptyMap(),
        sessionCount = 0L,
        sessionCountByType = emptyMap(),
        averageSessionDurationMillis = 0L,
        averageSessionDurationByType = emptyMap(),
        longestSessionDurationMillis = 0L,
        longestSessionDurationByType = emptyMap(),
        activeDays = 0,
        activeDaysByType = emptyMap(),
        trend = emptyList(),
        navigationTrend = emptyList(),
        topTitles = emptyList(),
        trackingStartedAtEpochMillis = null,
        trackingStartDate = null,
        earlierDurationMillis = 0L,
        earlierDurationByType = emptyMap(),
    )
}
