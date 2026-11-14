package eu.kanade.presentation.more.stats.recap.list

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLocale
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.rounded.AutoStories
import app.katari.presentation.kit.content.Badge
import app.katari.presentation.kit.content.BadgeTone
import app.katari.presentation.kit.content.Icon
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.feedback.EmptyScreen
import app.katari.presentation.kit.layout.RowList
import app.katari.presentation.kit.layout.TileGrid
import app.katari.presentation.kit.list.RowGroupGap
import app.katari.presentation.kit.list.RowGroupHeader
import app.katari.presentation.kit.surface.DashboardTile
import app.katari.presentation.kit.surface.StoryBanner
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import eu.kanade.presentation.more.stats.components.StatisticsShareBar
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.tachiyomi.ui.stats.recap.list.StatisticsRecapsViewModel
import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import eu.kanade.tachiyomi.ui.stats.recap.recapPeriodTitle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.plus
import java.time.format.DateTimeFormatter

/** Years as banners in the recap's colours, then each year's finished months as tiles with their time. */
@Composable
fun StatisticsRecapsScreenContent(
    state: StatisticsRecapsViewModel.State,
    paddingValues: PaddingValues,
    onOpen: (StatisticsRecapPeriod) -> Unit,
) {
    if (state.years.isEmpty() && state.monthsByYear.isEmpty()) {
        EmptyScreen(MR.strings.statistics_recaps_empty, Modifier.padding(paddingValues))
        return
    }
    val formatDuration = rememberStatisticsDurationFormatter()
    RowList(
        contentPadding = paddingValues + PaddingValues(top = KatariTheme.spacing.m, bottom = KatariTheme.spacing.xl),
    ) {
        state.years.forEach { year ->
            item(key = "year-${year.year}") {
                Column {
                    YearRecapBanner(year, isNew = year == state.unopened, onOpen = { onOpen(year) })
                    RowGroupGap()
                }
            }
        }
        state.monthsByYear.forEach { monthsOfYear ->
            item(key = "months-header-${monthsOfYear.year}") { RowGroupHeader(monthsOfYear.year.toString()) }
            item(key = "months-${monthsOfYear.year}") {
                MonthTiles(monthsOfYear.months, formatDuration, onOpen)
            }
        }
    }
}

@Composable
private fun YearRecapBanner(year: StatisticsRecapPeriod.Year, isNew: Boolean, onOpen: () -> Unit) {
    StoryBanner(onClick = onOpen) {
        Icon(MaterialSymbols.Rounded.AutoStories, contentDescription = null, tone = ContentTone.Primary)
        Column(Modifier.weight(1f)) {
            Text(text = recapPeriodTitle(year), role = TextRole.SectionTitle)
            Text(text = stringResource(MR.strings.statistics_recap_banner_text), role = TextRole.Supporting)
        }
        if (isNew) Badge(text = stringResource(MR.strings.statistics_recap_new), tone = BadgeTone.Primary)
    }
}

/** A year's months, each with a bar against the year's busiest month so they differ at a glance. */
@Composable
private fun MonthTiles(
    months: List<StatisticsRecapsViewModel.RecapMonth>,
    formatDuration: (Long) -> String,
    onOpen: (StatisticsRecapPeriod) -> Unit,
) {
    val locale = LocalLocale.current.platformLocale
    val monthName = remember(locale) { DateTimeFormatter.ofPattern("LLL", locale) }
    val busiest = months.maxOf { it.durationMillis }.coerceAtLeast(1L)
    TileGrid(
        tiles = months.map { month ->
            { modifier ->
                DashboardTile(modifier = modifier, onClick = { onOpen(month.period) }) {
                    Text(text = month.period.month.format(monthName), role = TextRole.SectionTitle, maxLines = 1)
                    Text(
                        text = formatDuration(month.durationMillis),
                        role = TextRole.Supporting,
                        tone = ContentTone.Secondary,
                        maxLines = 1,
                    )
                    StatisticsShareBar(
                        fraction = month.durationMillis.toFloat() / busiest,
                        color = KatariTheme.chart.series,
                        modifier = Modifier.padding(top = KatariTheme.spacing.s),
                    )
                }
            }
        },
    )
}
