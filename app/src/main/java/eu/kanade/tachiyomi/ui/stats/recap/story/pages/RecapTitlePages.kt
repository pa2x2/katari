package eu.kanade.tachiyomi.ui.stats.recap.story.pages

import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapIndex
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import eu.kanade.tachiyomi.ui.stats.recap.story.TopTitleMoment
import java.time.LocalDate
import kotlin.math.roundToInt

internal fun StatisticsRecapIndex.openingPage(): StatisticsRecapPage.Opening =
    StatisticsRecapPage.Opening(period.end.year, shownTitles.take(OPENING_COVERS))

internal fun StatisticsRecapIndex.topTitlePage(): StatisticsRecapPage.TopTitle? {
    val top = shownTitles.firstOrNull() ?: return null
    val days = segments.filter { it.entryId == top.entryId }.map { LocalDate.parse(it.localDate) }.toSortedSet()
    val startedInPeriod = activity.firstActiveDateByEntry[top.entryId]
        ?.let { !LocalDate.parse(it).isBefore(period.start) } == true
    val finishedOn = if (top.entryId in activity.finishedEntryIds) {
        activity.completions.filter { it.entryId == top.entryId }.maxOfOrNull { LocalDate.parse(it.localDate) }
    } else {
        null
    }
    return StatisticsRecapPage.TopTitle(
        title = top,
        consumed = consumedCounts(setOf(top.entryId)).firstOrNull(),
        moment = TopTitleMoment(
            startedOn = days.first().takeIf { startedInPeriod },
            finishedOn = finishedOn,
            activeDays = days.size,
        ),
    )
}

/** Shown only with a full top five, so the page never lists a lonely #2. */
internal fun StatisticsRecapIndex.topFivePage(): StatisticsRecapPage.TopFive? {
    val topFive = shownTitles.take(5).takeIf { it.size == 5 } ?: return null
    return StatisticsRecapPage.TopFive(
        titles = topFive.drop(1),
        sharePercent = (topFive.sumOf { it.durationMillis } * 100.0 / totalMillis).roundToInt(),
    )
}

private const val OPENING_COVERS = 15
