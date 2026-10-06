package eu.kanade.tachiyomi.ui.stats.recap.story.pages

import eu.kanade.tachiyomi.ui.stats.days
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapIndex
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapSummary
import eu.kanade.tachiyomi.ui.stats.recap.story.SummaryFigure

/** The card a recap is shared as: its total, top five and the four most telling figures it has. */
internal fun StatisticsRecapIndex.summary(): StatisticsRecapSummary = StatisticsRecapSummary(
    durationMillis = totalMillis,
    titles = shownTitles.take(SUMMARY_TITLES),
    figures = listOfNotNull(
        longestRun()?.let { SummaryFigure.LongestRun(it.days().toInt()) },
        activity.finishedEntryIds.size.takeIf { it > 0 }?.let(SummaryFigure::Finished),
        topHour()?.let(SummaryFigure::TopHour),
        genresPage()?.genres?.firstOrNull()?.let { SummaryFigure.TopGenre(it.name) },
        activeDates.size.takeIf { it > 0 }?.let(SummaryFigure::ActiveDays),
        consumedCounts().firstOrNull()?.let(SummaryFigure::Consumed),
    ).take(SUMMARY_FIGURES),
)

private const val SUMMARY_TITLES = 5
private const val SUMMARY_FIGURES = 4
