package eu.kanade.tachiyomi.ui.stats.recap.story.pages

import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import eu.kanade.tachiyomi.ui.stats.recap.period.lengthInDays
import eu.kanade.tachiyomi.ui.stats.recap.story.ComparedPeriod
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapIndex
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import tachiyomi.domain.statistics.recap.StatisticsRecapPeriodTotals
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * @param previous the same span of the period before, with its totals.
 */
internal fun StatisticsRecapIndex.comparisonPage(
    previous: Pair<StatisticsRecapPeriod, StatisticsRecapPeriodTotals>?,
): StatisticsRecapPage.Comparison? {
    val (previousPeriod, totals) = previous ?: return null
    if (totals.durationMillis <= 0L || totalMillis <= 0L || !wasTrackedThrough(previousPeriod)) return null
    val top = shownTitles.firstOrNull()
    return StatisticsRecapPage.Comparison(
        previous = when (previousPeriod) {
            is StatisticsRecapPeriod.Year -> ComparedPeriod.Year(previousPeriod.year)
            is StatisticsRecapPeriod.Month -> ComparedPeriod.Month(previousPeriod.month)
            is StatisticsRecapPeriod.Window -> return null
        },
        changePercent = ((totalMillis - totals.durationMillis) * 100.0 / totals.durationMillis).roundToInt(),
        sameTopTitle = top?.takeIf { it.entryId == totals.topEntryId },
    )
}

/**
 * Whether activity was recorded for nearly all of [period]. A period that tracking began partway through would make
 * any comparison with it look like a jump.
 */
internal fun StatisticsRecapIndex.wasTrackedThrough(period: StatisticsRecapPeriod): Boolean {
    val firstActive = activity.profileFirstActiveDate?.let(LocalDate::parse) ?: return false
    val untrackedDays = (firstActive.toEpochDay() - period.start.toEpochDay()).coerceAtLeast(0L)
    return untrackedDays * UNTRACKED_DIVISOR <= period.lengthInDays
}

// At most a tenth of a compared period may predate tracking.
private const val UNTRACKED_DIVISOR = 10L
