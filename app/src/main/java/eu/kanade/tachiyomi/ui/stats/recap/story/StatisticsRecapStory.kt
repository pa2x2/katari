package eu.kanade.tachiyomi.ui.stats.recap.story

import androidx.compose.runtime.Immutable
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.biggestDayPage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.bookendPage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.comparisonPage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.finishedPage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.genresPage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.hoursPage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.longestRunPage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.monthHighlightsPage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.monthTimePage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.monthTitlesPage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.monthTotalPage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.newTitlesPage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.openingPage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.summary
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.topFivePage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.topTitlePage
import eu.kanade.tachiyomi.ui.stats.recap.story.pages.typesPage
import mihon.entry.interactions.statistics.EntryStatisticsContribution
import tachiyomi.domain.statistics.recap.StatisticsRecapActivity
import tachiyomi.domain.statistics.recap.StatisticsRecapPeriodTotals

/** A recap as pages in the order they're shown; always ends with the summary page. */
@Immutable
data class StatisticsRecapStory(
    val period: StatisticsRecapPeriod,
    val pages: List<StatisticsRecapPage>,
) {
    val summary: StatisticsRecapSummary get() = (pages.last() as StatisticsRecapPage.Summary).summary
}

/**
 * Builds the story of [period]. Every page decides from the activity alone whether it has something to tell, so a
 * quiet or partly tracked period just gets a shorter story.
 *
 * @param previous the same span of the period before with its totals, or null when there is none.
 * @param hiddenEntryIds titles to leave unnamed and unshown; their time still counts.
 */
fun buildStatisticsRecapStory(
    period: StatisticsRecapPeriod,
    activity: StatisticsRecapActivity,
    previous: Pair<StatisticsRecapPeriod, StatisticsRecapPeriodTotals>?,
    hiddenEntryIds: Set<Long>,
    contributions: List<EntryStatisticsContribution>,
): StatisticsRecapStory {
    val scoped = (period as? StatisticsRecapPeriod.Window)?.type?.let { activity.onlyType(it) } ?: activity
    val index = StatisticsRecapIndex(period, scoped, hiddenEntryIds, contributions)
    val pages = when (period) {
        is StatisticsRecapPeriod.Year -> with(index) {
            listOfNotNull(
                openingPage(),
                StatisticsRecapPage.TotalTime(totalMillis),
                bookendPage(isFirst = true),
                topTitlePage(),
                topFivePage(),
                monthTitlesPage(),
                monthTimePage(),
                biggestDayPage(),
                hoursPage(),
                longestRunPage(),
                finishedPage(),
                newTitlesPage(),
                typesPage(),
                genresPage(),
                comparisonPage(previous),
                bookendPage(isFirst = false),
            )
        }
        is StatisticsRecapPeriod.Month -> with(index) {
            listOfNotNull(monthTotalPage(period, previous), monthHighlightsPage())
        }
        is StatisticsRecapPeriod.Window -> emptyList()
    }
    return StatisticsRecapStory(period, pages + StatisticsRecapPage.Summary(index.summary()))
}
