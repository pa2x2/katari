package eu.kanade.tachiyomi.ui.stats.recap.story.pages

import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapIndex
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import tachiyomi.domain.statistics.recap.StatisticsRecapPeriodTotals

/** @param previous the month before, with its totals; compared only when it was tracked throughout. */
internal fun StatisticsRecapIndex.monthTotalPage(
    month: StatisticsRecapPeriod.Month,
    previous: Pair<StatisticsRecapPeriod, StatisticsRecapPeriodTotals>?,
): StatisticsRecapPage.MonthTotal = StatisticsRecapPage.MonthTotal(
    month = month.month,
    durationMillis = totalMillis,
    previousDurationMillis = previous
        ?.takeIf { (period, totals) -> totals.durationMillis > 0L && wasTrackedThrough(period) }
        ?.second?.durationMillis,
    topTitle = shownTitles.firstOrNull(),
)

internal fun StatisticsRecapIndex.monthHighlightsPage(): StatisticsRecapPage.MonthHighlights? {
    val titles = shownTitles.take(MONTH_TOP_TITLES).takeIf { it.isNotEmpty() } ?: return null
    return StatisticsRecapPage.MonthHighlights(titles, biggestDayPage() ?: return null)
}

private const val MONTH_TOP_TITLES = 3
