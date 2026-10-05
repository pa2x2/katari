package eu.kanade.tachiyomi.ui.stats.recap.delivery

import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import java.time.LocalDate
import java.time.Month

/**
 * The year recap that is new on [today]: the year so far through December, then the whole year through January.
 * Null the rest of the year, when past recaps are only in the Recaps list.
 */
fun newYearRecap(today: LocalDate): StatisticsRecapPeriod.Year? = when (today.month) {
    Month.DECEMBER -> StatisticsRecapPeriod.Year(today.year, end = today)
    Month.JANUARY -> StatisticsRecapPeriod.Year(today.year - 1)
    else -> null
}

/**
 * Identifies a year recap edition for "opened" bookkeeping: the December edition and the final one differ, so the
 * final recap is new again in January after the year so far was opened.
 */
val StatisticsRecapPeriod.Year.editionKey: String get() = if (isSoFar) "$year-so-far" else "$year"

/** The year recaps listed on [today]: every past year, and the current one only once December has begun. */
fun listedRecapYears(activeYears: Collection<Int>, today: LocalDate): List<StatisticsRecapPeriod.Year> =
    activeYears.filter { it < today.year || (it == today.year && today.month == Month.DECEMBER) }
        .sortedDescending()
        .map { year -> StatisticsRecapPeriod.Year(year, end = minOf(today, LocalDate.of(year, 12, 31))) }
