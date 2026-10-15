package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.recap.components.RecapCover
import eu.kanade.presentation.more.stats.recap.components.RecapKicker
import eu.kanade.presentation.more.stats.recap.components.RecapText
import eu.kanade.presentation.more.stats.recap.components.recapTitleTarget
import eu.kanade.presentation.more.stats.recap.components.recapTypeName
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.presentation.more.stats.recap.motion.recapFloat
import eu.kanade.presentation.more.stats.recap.motion.recapPulse
import eu.kanade.presentation.more.stats.recap.motion.recapReveal
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapTitle
import eu.kanade.tachiyomi.ui.stats.recap.story.TopTitleMoment
import mihon.presentation.kit.content.StoryMetrics
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.time.LocalDate

/** The year in large type under the period's covers, which the page draws behind it as a drifting wall. */
@Composable
internal fun ColumnScope.RecapOpeningPage(page: StatisticsRecapPage.Opening) {
    Spacer(Modifier.weight(1f))
    RecapKicker(stringResource(MR.strings.statistics_recap_year_title), order = 1)
    RecapText(page.year.toString(), TextRole.Hero, order = 2)
    RecapText(
        text = stringResource(MR.strings.statistics_recap_tap_to_start),
        role = TextRole.Body,
        order = 3,
        modifier = Modifier.recapPulse(),
    )
}

@Composable
internal fun ColumnScope.RecapTopTitlePage(page: StatisticsRecapPage.TopTitle) {
    val formatDuration = rememberStatisticsDurationFormatter()
    val formats = rememberRecapFormats()
    RecapKicker(stringResource(MR.strings.statistics_recap_top_kicker))
    Spacer(Modifier.weight(1f))
    RecapCover(
        title = page.title,
        width = StoryMetrics.coverFeature,
        modifier = Modifier.align(Alignment.CenterHorizontally).recapReveal(1).recapFloat(),
    )
    RecapText(
        text = page.title.title,
        role = TextRole.Metric,
        textAlign = TextAlign.Center,
        maxLines = 2,
        order = 2,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xxl, Alignment.CenterHorizontally),
        modifier = Modifier.fillMaxWidth().recapReveal(3),
    ) {
        RecapFigure(
            formatDuration(page.title.durationMillis),
            stringResource(MR.strings.statistics_time_spent).lowercase(),
        )
        page.consumed?.let {
            RecapFigure(formats.number(it.count), stringResource(it.unitLabel).lowercase())
        }
    }
    RecapText(
        text = topTitleMomentText(page.moment, formats::dayMonth),
        role = TextRole.Body,
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
        verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
        modifier = Modifier.padding(top = KatariTheme.spacing.s),
    ) {
        page.titles.forEachIndexed { index, title ->
            RecapRankRow(rank = index + 2, title = title, order = index + 1)
        }
    }
    Spacer(Modifier.weight(1f))
    RecapText(
        text = stringResource(MR.strings.statistics_recap_top_five_share, page.sharePercent),
        role = TextRole.Body,
        order = 5,
    )
}

/** One ranked title: its place, cover, name and time with its type. */
@Composable
internal fun RecapRankRow(rank: Int, title: StatisticsRecapTitle, order: Int) {
    val formatDuration = rememberStatisticsDurationFormatter()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
        modifier = Modifier.recapReveal(order).recapTitleTarget(title.entryId),
    ) {
        RecapText(
            text = rank.toString(),
            role = TextRole.Metric,
            tone = ContentTone.Primary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(StoryMetrics.rankWidth),
        )
        RecapCover(title, width = StoryMetrics.coverRow)
        Column(Modifier.weight(1f)) {
            RecapText(title.title, TextRole.SectionTitle, maxLines = 2)
            RecapText(
                text = "${formatDuration(title.durationMillis)} · ${recapTypeName(title.type)}",
                role = TextRole.Supporting,
                tone = ContentTone.Secondary,
            )
        }
    }
}

@Composable
private fun RecapFigure(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        RecapText(value, TextRole.SheetTitle)
        RecapText(label, TextRole.Supporting)
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
