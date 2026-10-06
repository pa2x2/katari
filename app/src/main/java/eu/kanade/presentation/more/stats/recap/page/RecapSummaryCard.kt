package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.recap.components.LocalRecapPalette
import eu.kanade.presentation.more.stats.recap.components.RecapCover
import eu.kanade.presentation.more.stats.recap.components.RecapFormats
import eu.kanade.presentation.more.stats.recap.components.RecapKicker
import eu.kanade.presentation.more.stats.recap.components.RecapText
import eu.kanade.presentation.more.stats.recap.components.RecapTypography
import eu.kanade.presentation.more.stats.recap.components.recapTitleTarget
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.presentation.more.stats.recap.motion.recapReveal
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapSummary
import eu.kanade.tachiyomi.ui.stats.recap.story.SummaryFigure
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** The card a recap is shared as: the total, the top five and up to four figures, readable at a glance. */
@Composable
internal fun ColumnScope.RecapSummaryCard(summary: StatisticsRecapSummary, periodTitle: String) {
    val formats = rememberRecapFormats()
    val formatDuration = rememberStatisticsDurationFormatter()
    val palette = LocalRecapPalette.current
    RecapKicker(stringResource(MR.strings.statistics_recap_summary_kicker, periodTitle))
    RecapTotalCountUp(summary.durationMillis, order = 0, compact = true)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.recapReveal(1)) {
        summary.titles.forEachIndexed { index, title ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.recapTitleTarget(title.entryId),
            ) {
                RecapText(
                    text = (index + 1).toString(),
                    style = RecapTypography.Title,
                    color = palette.accent,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(18.dp),
                )
                RecapCover(title, width = 30.dp)
                RecapText(title.title, RecapTypography.RowTitle, maxLines = 1, modifier = Modifier.weight(1f))
                RecapText(formatDuration(title.durationMillis), RecapTypography.Small, color = palette.muted)
            }
        }
    }
    Spacer(Modifier.weight(1f))
    summary.figures.chunked(2).forEachIndexed { row, figures ->
        Row(modifier = Modifier.recapReveal(row + 2)) {
            figures.forEach { figure ->
                Column(Modifier.weight(1f)) {
                    RecapText(figureValue(figure, formats), RecapTypography.Title, maxLines = 1)
                    RecapText(figureLabel(figure), RecapTypography.Small, color = palette.muted, maxLines = 1)
                }
            }
            if (figures.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun figureValue(figure: SummaryFigure, formats: RecapFormats): String = when (figure) {
    is SummaryFigure.LongestRun -> pluralStringResource(MR.plurals.day, figure.days, figure.days)
    is SummaryFigure.Finished -> formats.number(figure.count.toLong())
    is SummaryFigure.TopHour -> formats.hour(figure.hour)
    is SummaryFigure.TopGenre -> figure.name
    is SummaryFigure.ActiveDays -> formats.number(figure.days.toLong())
    is SummaryFigure.Consumed -> formats.number(figure.count.count)
}

@Composable
private fun figureLabel(figure: SummaryFigure): String = when (figure) {
    is SummaryFigure.LongestRun -> stringResource(MR.strings.statistics_recap_figure_longest_run)
    is SummaryFigure.Finished -> stringResource(MR.strings.statistics_recap_figure_finished)
    is SummaryFigure.TopHour -> stringResource(MR.strings.statistics_recap_figure_hour)
    is SummaryFigure.TopGenre -> stringResource(MR.strings.statistics_recap_figure_genre)
    is SummaryFigure.ActiveDays -> stringResource(MR.strings.statistics_recap_figure_active_days)
    is SummaryFigure.Consumed -> stringResource(figure.count.unitLabel).lowercase()
}
