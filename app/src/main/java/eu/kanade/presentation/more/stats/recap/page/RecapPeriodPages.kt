package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.stats.components.rememberStatisticsDurationFormatter
import eu.kanade.presentation.more.stats.recap.components.HOUR_MILLIS
import eu.kanade.presentation.more.stats.recap.components.MINUTE_MILLIS
import eu.kanade.presentation.more.stats.recap.components.RecapCover
import eu.kanade.presentation.more.stats.recap.components.RecapDayRings
import eu.kanade.presentation.more.stats.recap.components.RecapKicker
import eu.kanade.presentation.more.stats.recap.components.RecapText
import eu.kanade.presentation.more.stats.recap.components.RecapTotal
import eu.kanade.presentation.more.stats.recap.components.recapComparedPeriodText
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.presentation.more.stats.recap.motion.RecapCountUp
import eu.kanade.presentation.more.stats.recap.motion.recapFloat
import eu.kanade.presentation.more.stats.recap.motion.recapReveal
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import mihon.presentation.kit.content.StoryMetrics
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
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
        RecapDayRings(page.durationMillis, order = 1, modifier = Modifier.recapReveal(1))
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
            role = TextRole.Body,
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
    change?.let { RecapText(text = it, role = TextRole.Body, order = 2) }
    Spacer(Modifier.weight(1f))
    page.topTitle?.let { top ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            modifier = Modifier.recapReveal(3),
        ) {
            RecapCover(top, width = StoryMetrics.coverMedium, modifier = Modifier.recapFloat())
            RecapText(
                text = stringResource(MR.strings.statistics_recap_most_of_it, top.title),
                role = TextRole.Body,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
internal fun ColumnScope.RecapMonthHighlightsPage(page: StatisticsRecapPage.MonthHighlights) {
    val formats = rememberRecapFormats()
    RecapKicker(stringResource(MR.strings.statistics_recap_month_kicker))
    Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m)) {
        page.titles.forEachIndexed { index, title -> RecapRankRow(rank = index + 1, title = title, order = index + 1) }
    }
    Spacer(Modifier.weight(1f))
    RecapKicker(stringResource(MR.strings.statistics_recap_biggest_day_kicker), order = 4)
    RecapText(formats.weekdayDayMonth(page.biggestDay.date), TextRole.Metric, order = 4)
    RecapText(biggestDayText(page.biggestDay, formats), TextRole.Body, order = 5)
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
    RecapText("$sign${page.changePercent.absoluteValue}%", TextRole.Hero, order = 1)
    RecapText(
        text = stringResource(
            when {
                page.changePercent > 0 -> MR.strings.statistics_recap_more_time
                page.changePercent < 0 -> MR.strings.statistics_recap_less_time
                else -> MR.strings.statistics_recap_same_time
            },
            previous,
        ),
        role = TextRole.Body,
        order = 2,
    )
    Spacer(Modifier.weight(1f))
    page.sameTopTitle?.let { top ->
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.l),
            modifier = Modifier.recapReveal(3),
        ) {
            RecapCover(top, width = StoryMetrics.coverMedium)
            RecapText(
                text = stringResource(MR.strings.statistics_recap_same_top, top.title),
                role = TextRole.Body,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * A period's [RecapTotal] counting up. The unit sits apart from the number, smaller, so the number keeps its size; on
 * its own line the number shrinks only when it can't fit the page's width.
 *
 * @param compact sets the unit beside the number at a smaller size, for the summary card.
 */
@Composable
internal fun RecapTotalCountUp(durationMillis: Long, order: Int, compact: Boolean = false) {
    val total = RecapTotal(durationMillis)
    val value = total.value
    val formats = rememberRecapFormats()
    // Under a minute there is nothing to count up to, so the bound shows from the start.
    val lessThanMinute = stringResource(MR.strings.statistics_recap_less_than, formats.number(value))
    val format: (Long) -> String = if (total.underMinute) { _ -> lessThanMinute } else formats::number
    val unit = pluralStringResource(
        if (total.inHours) MR.plurals.statistics_recap_hours_unit else MR.plurals.statistics_recap_minutes_unit,
        value.toInt(),
    )
    if (compact) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.m),
            modifier = Modifier.recapReveal(order),
        ) {
            RecapCountUp(
                value = value,
                format = format,
                role = TextRole.Display,
                order = order,
                modifier = Modifier.alignByBaseline(),
            )
            RecapText(unit, TextRole.SheetTitle, modifier = Modifier.alignByBaseline())
        }
    } else {
        Column(Modifier.recapReveal(order)) {
            RecapCountUp(
                value = value,
                format = format,
                role = TextRole.Hero,
                order = order,
                shrinkTo = TextRole.Display,
            )
            RecapText(unit, TextRole.Metric)
        }
    }
}

private const val DAY_MILLIS = 24 * HOUR_MILLIS
