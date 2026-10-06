package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.recap.components.HOUR_MILLIS
import eu.kanade.presentation.more.stats.recap.components.MINUTE_MILLIS
import eu.kanade.presentation.more.stats.recap.components.RecapCover
import eu.kanade.presentation.more.stats.recap.components.RecapKicker
import eu.kanade.presentation.more.stats.recap.components.RecapText
import eu.kanade.presentation.more.stats.recap.components.RecapTypography
import eu.kanade.presentation.more.stats.recap.components.recapComparedPeriodText
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.presentation.more.stats.recap.motion.RecapCountUp
import eu.kanade.presentation.more.stats.recap.motion.recapFloat
import eu.kanade.presentation.more.stats.recap.motion.recapReveal
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.absoluteValue

@Composable
internal fun ColumnScope.RecapTotalTimePage(page: StatisticsRecapPage.TotalTime, periodTitle: String) {
    RecapKicker(stringResource(MR.strings.statistics_recap_spent_in, periodTitle))
    Spacer(Modifier.weight(1f))
    RecapTotalCountUp(page.durationMillis, order = 1)
    val days = (page.durationMillis / DAY_MILLIS).toInt()
    val hours = (page.durationMillis % DAY_MILLIS / HOUR_MILLIS).toInt()
    if (days > 0) {
        RecapText(
            text = if (hours > 0) {
                stringResource(
                    MR.strings.statistics_recap_days_and_hours,
                    pluralStringResource(MR.plurals.day, days, days),
                    pluralStringResource(MR.plurals.statistics_recap_hours, hours, hours),
                )
            } else {
                pluralStringResource(MR.plurals.statistics_recap_full_days, days, days)
            },
            style = RecapTypography.Body,
            order = 2,
        )
    }
    Spacer(Modifier.weight(1.4f))
}

/** A month's opening: its total against the month before, and where most of it went. */
@Composable
internal fun ColumnScope.RecapMonthTotalPage(page: StatisticsRecapPage.MonthTotal) {
    val formats = rememberRecapFormats()
    val formatDuration = rememberStatisticsDurationFormatter()
    RecapKicker(stringResource(MR.strings.statistics_recap_spent_in, formats.month(page.month)))
    Spacer(Modifier.weight(1f))
    RecapTotalCountUp(page.durationMillis, order = 1)
    val change = page.previousDurationMillis?.let { previous ->
        val previousMonth = formats.month(page.month.minusMonths(1L))
        val difference = page.durationMillis - previous
        val amount = formatDuration(difference.absoluteValue)
        when {
            difference.absoluteValue < MINUTE_MILLIS -> null
            difference > 0L -> stringResource(MR.strings.statistics_recap_month_more, amount, previousMonth)
            else -> stringResource(MR.strings.statistics_recap_month_less, amount, previousMonth)
        }
    }
    change?.let { RecapText(text = it, style = RecapTypography.Body, order = 2) }
    Spacer(Modifier.weight(1f))
    page.topTitle?.let { top ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.recapReveal(3),
        ) {
            RecapCover(top, width = 72.dp, modifier = Modifier.recapFloat())
            RecapText(
                text = stringResource(MR.strings.statistics_recap_most_of_it, top.title),
                style = RecapTypography.Body,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun ColumnScope.RecapMonthHighlightsPage(page: StatisticsRecapPage.MonthHighlights) {
    val formats = rememberRecapFormats()
    RecapKicker(stringResource(MR.strings.statistics_recap_month_kicker))
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        page.titles.forEachIndexed { index, title -> RecapRankRow(rank = index + 1, title = title, order = index + 1) }
    }
    Spacer(Modifier.weight(1f))
    RecapKicker(stringResource(MR.strings.statistics_recap_biggest_day_kicker), order = 4)
    RecapText(formats.weekdayDayMonth(page.biggestDay.date), RecapTypography.Headline, order = 4)
    RecapText(biggestDayText(page.biggestDay, formats), RecapTypography.Body, order = 5)
}

@Composable
internal fun ColumnScope.RecapComparisonPage(page: StatisticsRecapPage.Comparison) {
    val formats = rememberRecapFormats()
    val previous = recapComparedPeriodText(page.previous, formats)
    RecapKicker(stringResource(MR.strings.statistics_recap_compared_kicker, previous))
    Spacer(Modifier.weight(1f))
    val sign = if (page.changePercent > 0) {
        "+"
    } else if (page.changePercent < 0) {
        "−"
    } else {
        ""
    }
    RecapText("$sign${page.changePercent.absoluteValue}%", RecapTypography.Huge, order = 1)
    RecapText(
        text = stringResource(
            when {
                page.changePercent > 0 -> MR.strings.statistics_recap_more_time
                page.changePercent < 0 -> MR.strings.statistics_recap_less_time
                else -> MR.strings.statistics_recap_same_time
            },
            previous,
        ),
        style = RecapTypography.Body,
        order = 2,
    )
    Spacer(Modifier.weight(1f))
    page.sameTopTitle?.let { top ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.recapReveal(3),
        ) {
            RecapCover(top, width = 72.dp)
            RecapText(
                text = stringResource(MR.strings.statistics_recap_same_top, top.title),
                style = RecapTypography.Body,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * A period's total counting up: whole hours, or minutes below an hour. The unit sits apart from the number, smaller,
 * so the number keeps its size; on its own line the number shrinks only when it can't fit the page's width.
 *
 * @param compact sets the unit beside the number at a smaller size, for the summary card.
 */
@Composable
internal fun RecapTotalCountUp(durationMillis: Long, order: Int, compact: Boolean = false) {
    val inHours = durationMillis >= HOUR_MILLIS
    val value = if (inHours) durationMillis / HOUR_MILLIS else durationMillis / MINUTE_MILLIS
    val formats = rememberRecapFormats()
    val unit = pluralStringResource(
        if (inHours) MR.plurals.statistics_recap_hours_unit else MR.plurals.statistics_recap_minutes_unit,
        value.toInt(),
    )
    if (compact) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.recapReveal(order),
        ) {
            RecapCountUp(
                value = value,
                format = formats::number,
                style = RecapTypography.Display,
                order = order,
                modifier = Modifier.alignByBaseline(),
            )
            RecapText(unit, RecapTypography.Title, modifier = Modifier.alignByBaseline())
        }
    } else {
        Column(Modifier.recapReveal(order)) {
            RecapCountUp(
                value = value,
                format = formats::number,
                style = RecapTypography.Huge,
                order = order,
                autoSize = TextAutoSize.StepBased(
                    minFontSize = RecapTypography.Display.fontSize,
                    maxFontSize = RecapTypography.Huge.fontSize,
                ),
            )
            RecapText(unit, RecapTypography.Headline)
        }
    }
}

private const val DAY_MILLIS = 24 * HOUR_MILLIS
