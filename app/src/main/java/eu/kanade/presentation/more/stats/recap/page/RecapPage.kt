package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.stats.recap.palette.RecapPalette
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage

/**
 * One recap page in its frame.
 *
 * @param periodTitle how the period reads on pages that name it, such as "2026" or "September 2026".
 * @param footer the line at the foot of the page.
 */
@Composable
internal fun RecapPage(
    page: StatisticsRecapPage,
    palette: RecapPalette,
    periodTitle: String,
    footer: String,
    modifier: Modifier = Modifier,
) {
    RecapPageFrame(palette = palette, footer = footer, modifier = modifier) {
        when (page) {
            is StatisticsRecapPage.Opening -> RecapOpeningPage(page)
            is StatisticsRecapPage.TotalTime -> RecapTotalTimePage(page, periodTitle)
            is StatisticsRecapPage.Bookend -> RecapBookendPage(page)
            is StatisticsRecapPage.TopTitle -> RecapTopTitlePage(page)
            is StatisticsRecapPage.TopFive -> RecapTopFivePage(page)
            is StatisticsRecapPage.MonthTitles -> RecapMonthTitlesPage(page)
            is StatisticsRecapPage.MonthTime -> RecapMonthTimePage(page)
            is StatisticsRecapPage.BiggestDay -> RecapBiggestDayPage(page)
            is StatisticsRecapPage.Hours -> RecapHoursPage(page)
            is StatisticsRecapPage.LongestRun -> RecapLongestRunPage(page)
            is StatisticsRecapPage.Finished -> RecapFinishedPage(page)
            is StatisticsRecapPage.NewTitles -> RecapNewTitlesPage(page)
            is StatisticsRecapPage.Types -> RecapTypesPage(page)
            is StatisticsRecapPage.Genres -> RecapGenresPage(page)
            is StatisticsRecapPage.Comparison -> RecapComparisonPage(page)
            is StatisticsRecapPage.MonthTotal -> RecapMonthTotalPage(page)
            is StatisticsRecapPage.MonthHighlights -> RecapMonthHighlightsPage(page)
            is StatisticsRecapPage.Summary -> RecapSummaryCard(page.summary, periodTitle)
        }
    }
}
