package eu.kanade.presentation.more.stats.recap.page

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.stats.recap.components.LocalRecapPalette
import eu.kanade.presentation.more.stats.recap.components.RecapBars
import eu.kanade.presentation.more.stats.recap.components.RecapCover
import eu.kanade.presentation.more.stats.recap.components.RecapHourClock
import eu.kanade.presentation.more.stats.recap.components.RecapKicker
import eu.kanade.presentation.more.stats.recap.components.RecapText
import eu.kanade.presentation.more.stats.recap.components.RecapTypography
import eu.kanade.presentation.more.stats.recap.components.calendar.RecapRunCalendar
import eu.kanade.presentation.more.stats.recap.components.recapDurationText
import eu.kanade.presentation.more.stats.recap.components.rememberRecapFormats
import eu.kanade.presentation.more.stats.recap.motion.RecapCountUp
import eu.kanade.presentation.more.stats.recap.motion.recapReveal
import eu.kanade.tachiyomi.ui.stats.days
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapPage
import tachiyomi.core.common.i18n.pluralStringResource
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource

/** Every month of the period with the title that led it. */
@Composable
internal fun ColumnScope.RecapMonthTitlesPage(page: StatisticsRecapPage.MonthTitles) {
    val formats = rememberRecapFormats()
    val palette = LocalRecapPalette.current
    RecapKicker(stringResource(MR.strings.statistics_recap_months_kicker))
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        page.months.chunked(MONTH_COLUMNS).forEachIndexed { row, months ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.recapReveal(row + 1)) {
                months.forEach { (month, title) ->
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        RecapText(formats.shortMonth(month), RecapTypography.Tiny, color = palette.muted)
                        if (title != null) {
                            RecapCover(title, width = 64.dp)
                        } else {
                            Box(
                                Modifier
                                    .width(64.dp)
                                    .aspectRatio(2f / 3f)
                                    .background(palette.faint, RoundedCornerShape(4.dp)),
                            )
                        }
                        RecapText(title?.title.orEmpty(), RecapTypography.Tiny, maxLines = 1)
                    }
                }
                repeat(MONTH_COLUMNS - months.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
    Spacer(Modifier.weight(1f))
    page.longestHold?.let { hold ->
        RecapText(
            text = stringResource(
                MR.strings.statistics_recap_month_hold,
                hold.title.title,
                formats.month(hold.from),
                formats.month(hold.to),
            ),
            style = RecapTypography.Body,
            order = 4,
        )
    }
}

@Composable
internal fun ColumnScope.RecapMonthTimePage(page: StatisticsRecapPage.MonthTime) {
    val formats = rememberRecapFormats()
    RecapKicker(stringResource(MR.strings.statistics_recap_months_kicker))
    Spacer(Modifier.weight(1f))
    RecapBars(
        values = page.months.map { it.second },
        labels = page.months.map { formats.narrowMonth(it.first) },
        highlight = page.months.indexOfFirst { it.first == page.biggest },
        height = 240.dp,
        order = 1,
    )
    Spacer(Modifier.weight(1f))
    RecapText(
        text = stringResource(MR.strings.statistics_recap_biggest_month, formats.month(page.biggest)),
        style = RecapTypography.Headline,
        order = 2,
    )
    val duration = recapDurationText(page.biggestDurationMillis)
    RecapText(
        text = page.biggestTitle
            ?.let { stringResource(MR.strings.statistics_recap_biggest_month_text, duration, it.title) }
            ?: "$duration.",
        style = RecapTypography.Body,
        order = 3,
    )
}

@Composable
internal fun ColumnScope.RecapLongestRunPage(page: StatisticsRecapPage.LongestRun) {
    val formats = rememberRecapFormats()
    val context = LocalContext.current
    val days = page.run.days().toInt()
    RecapKicker(stringResource(MR.strings.statistics_recap_run_kicker))
    RecapCountUp(
        value = days.toLong(),
        format = { context.pluralStringResource(MR.plurals.day, it.toInt(), it.toInt()) },
        style = RecapTypography.Display,
        order = 1,
        modifier = Modifier.recapReveal(1),
    )
    RecapText(
        text = stringResource(
            MR.strings.statistics_recap_run_dates,
            formats.dayMonth(page.run.start),
            formats.dayMonth(page.run.endInclusive),
        ) + " " + pluralStringResource(MR.plurals.statistics_recap_active_days_text, page.activeDays, page.activeDays),
        style = RecapTypography.Body,
        order = 2,
    )
    RecapRunCalendar(
        run = page.run,
        activeDates = page.activeDates,
        periodStart = page.periodStart,
        periodEnd = page.periodEnd,
        order = 4,
        modifier = Modifier.weight(1f).fillMaxWidth().recapReveal(3),
    )
}

@Composable
internal fun ColumnScope.RecapHoursPage(page: StatisticsRecapPage.Hours) {
    val formats = rememberRecapFormats()
    val topHourText = formats.hour(page.topHour)
    RecapKicker(stringResource(MR.strings.statistics_recap_hours_kicker))
    Spacer(Modifier.weight(1f))
    RecapHourClock(
        hourlyDurationMillis = page.hourlyDurationMillis,
        topHour = page.topHour,
        centerLabel = topHourText,
        hourLabel = formats::hour,
        size = 260.dp,
        order = 1,
        modifier = Modifier.align(Alignment.CenterHorizontally).recapReveal(1),
    )
    Spacer(Modifier.weight(1f))
    RecapText(stringResource(MR.strings.statistics_recap_your_hour, topHourText), RecapTypography.Headline, order = 2)
    RecapText(
        text = stringResource(
            MR.strings.statistics_recap_hour_window,
            page.windowPercent,
            formats.hour(page.windowStartHour),
            formats.hour((page.windowStartHour + WINDOW_HOURS) % HOURS_IN_DAY),
        ) + " " + stringResource(MR.strings.statistics_recap_busiest_weekday, formats.weekday(page.busiestWeekday)),
        style = RecapTypography.Body,
        order = 3,
        modifier = Modifier.fillMaxWidth(),
    )
}

private const val MONTH_COLUMNS = 4
private const val WINDOW_HOURS = 4
private const val HOURS_IN_DAY = 24
