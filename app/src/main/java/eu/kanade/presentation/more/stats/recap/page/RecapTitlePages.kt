package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.recap.components.LocalRecapPalette
import eu.kanade.presentation.more.stats.recap.components.RecapCover
import eu.kanade.presentation.more.stats.recap.components.RecapKicker
import eu.kanade.presentation.more.stats.recap.components.RecapText
import eu.kanade.presentation.more.stats.recap.components.RecapTypography
import eu.kanade.presentation.more.stats.recap.components.recapReveal
import eu.kanade.presentation.more.stats.recap.components.recapTitleTarget
import eu.kanade.presentation.more.stats.recap.components.recapTypeName
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapTitle
import eu.kanade.tachiyomi.ui.stats.recap.story.TopTitleMoment
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate

/** A tilted wall of the period's covers behind the year in large type. */
@Composable
internal fun ColumnScope.RecapOpeningPage(page: StatisticsRecapPage.Opening) {
    Box(Modifier.weight(1f).fillMaxWidth()) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .requiredWidth(470.dp)
                .offset(y = (-24).dp)
                .rotate(-8f)
                .alpha(0.6f),
        ) {
            page.covers.chunked(WALL_COLUMNS).forEachIndexed { row, covers ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.offset(x = if (row % 2 == 0) 0.dp else (-36).dp),
                ) {
                    covers.forEach { RecapCover(it, width = 84.dp, modifier = Modifier.recapReveal(row)) }
                }
            }
        }
    }
    RecapKicker(stringResource(MR.strings.statistics_recap_year_title), order = 1)
    RecapText(page.year.toString(), RecapTypography.Huge, order = 2)
    RecapText(stringResource(MR.strings.statistics_recap_tap_to_start), RecapTypography.Body, order = 3)
}

@Composable
internal fun ColumnScope.RecapTopTitlePage(page: StatisticsRecapPage.TopTitle) {
    val formatDuration = rememberStatisticsDurationFormatter()
    val formats = rememberRecapFormats()
    val muted = LocalRecapPalette.current.muted
    RecapKicker(stringResource(MR.strings.statistics_recap_top_kicker))
    Spacer(Modifier.weight(1f))
    RecapCover(
        title = page.title,
        width = 156.dp,
        modifier = Modifier.align(Alignment.CenterHorizontally).recapReveal(1),
    )
    RecapText(
        text = page.title.title,
        style = RecapTypography.Headline,
        textAlign = TextAlign.Center,
        maxLines = 2,
        order = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(32.dp, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth().recapReveal(3),
    ) {
        RecapFigure(
            formatDuration(page.title.durationMillis),
            stringResource(MR.strings.statistics_time_spent).lowercase(),
            muted,
        )
        page.consumed?.let {
            RecapFigure(formats.number(it.count), stringResource(it.unitLabel).lowercase(), muted)
        }
    }
    RecapText(
        text = topTitleMomentText(page.moment, formats::dayMonth),
        style = RecapTypography.Body,
        textAlign = TextAlign.Center,
        order = 4,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.weight(1f))
}

@Composable
internal fun ColumnScope.RecapTopFivePage(page: StatisticsRecapPage.TopFive) {
    RecapKicker(stringResource(MR.strings.statistics_recap_top_five_kicker))
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.padding(top = 8.dp),
    ) {
        page.titles.forEachIndexed { index, title ->
            RecapRankRow(rank = index + 2, title = title, order = index + 1)
        }
    }
    Spacer(Modifier.weight(1f))
    RecapText(
        text = stringResource(MR.strings.statistics_recap_top_five_share, page.sharePercent),
        style = RecapTypography.Body,
        order = 5,
    )
}

/** One ranked title: its place, cover, name and time with its type. */
@Composable
internal fun RecapRankRow(rank: Int, title: StatisticsRecapTitle, order: Int, coverWidth: Int = 46) {
    val formatDuration = rememberStatisticsDurationFormatter()
    val palette = LocalRecapPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.recapReveal(order).recapTitleTarget(title.entryId),
    ) {
        RecapText(
            text = rank.toString(),
            style = RecapTypography.Rank,
            color = palette.accent,
            textAlign = TextAlign.End,
            modifier = Modifier.width(30.dp),
        )
        RecapCover(title, width = coverWidth.dp)
        Column(Modifier.weight(1f)) {
            RecapText(title.title, RecapTypography.RowTitle, maxLines = 2)
            RecapText(
                text = "${formatDuration(title.durationMillis)} · ${recapTypeName(title.type)}",
                style = RecapTypography.Small,
                color = palette.muted,
            )
        }
    }
}

@Composable
private fun RecapFigure(value: String, label: String, labelColor: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RecapText(value, RecapTypography.Title)
        RecapText(label, RecapTypography.Small, color = labelColor)
    }
}

@Composable
private fun topTitleMomentText(moment: TopTitleMoment, formatDate: (LocalDate) -> String): String {
    val started = moment.startedOn
    val finished = moment.finishedOn
    return when {
        started != null && finished != null -> stringResource(
            MR.strings.statistics_recap_started_finished,
            formatDate(started),
            formatDate(finished),
        )
        started != null -> pluralStringResource(
            MR.plurals.statistics_recap_started_days_with,
            moment.activeDays,
            moment.activeDays,
            formatDate(started),
        )
        finished != null -> stringResource(MR.strings.statistics_recap_finished_on, formatDate(finished))
        else -> pluralStringResource(MR.plurals.statistics_recap_days_with, moment.activeDays, moment.activeDays)
    }
}

private const val WALL_COLUMNS = 5
