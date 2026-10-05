package eu.kanade.presentation.more.stats.recap.components

import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.presentation.util.formatChapterNumber
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.stats.recap.story.ComparedPeriod
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapConsumedCount
import eu.kanade.tachiyomi.ui.stats.recap.story.StatisticsRecapItems
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat
import java.time.DayOfWeek
import java.time.LocalTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAccessor
import java.util.Locale

/** Locale-aware formats recap pages share, built once per locale. */
internal class RecapFormats(val locale: Locale) {
    private val integer = NumberFormat.getIntegerInstance(locale)
    private val dayMonth = pattern("MMMMd")
    private val weekdayDayMonth = pattern("EEEEMMMMd")
    private val time = pattern("jmm")
    private val hour = pattern("j")
    private val monthYear = pattern("MMMMyyyy")

    fun number(value: Long): String = integer.format(value)

    fun dayMonth(date: TemporalAccessor): String = dayMonth.format(date)

    fun weekdayDayMonth(date: TemporalAccessor): String = weekdayDayMonth.format(date)

    fun time(time: LocalTime): String = this.time.format(time)

    fun hour(hour: Int): String = this.hour.format(LocalTime.of(hour, 0))

    fun month(month: YearMonth): String = month.month.getDisplayName(TextStyle.FULL_STANDALONE, locale)

    fun shortMonth(month: YearMonth): String = month.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale)

    fun narrowMonth(month: YearMonth): String = month.month.getDisplayName(TextStyle.NARROW_STANDALONE, locale)

    fun weekday(day: DayOfWeek): String = day.getDisplayName(TextStyle.FULL_STANDALONE, locale)

    fun monthYear(month: YearMonth): String = monthYear.format(month)

    private fun pattern(skeleton: String) =
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale)
}

@Composable
internal fun rememberRecapFormats(): RecapFormats {
    val locale = LocalConfiguration.current.locales[0]
    return remember(locale) { RecapFormats(locale) }
}

/** A period's large total: whole hours, or minutes below an hour. */
@Composable
internal fun recapDurationText(durationMillis: Long): String {
    val hours = (durationMillis / HOUR_MILLIS).toInt()
    return if (hours > 0) {
        pluralStringResource(MR.plurals.statistics_recap_hours, hours, hours)
    } else {
        val minutes = (durationMillis / MINUTE_MILLIS).toInt()
        pluralStringResource(MR.plurals.statistics_recap_minutes, minutes, minutes)
    }
}

@Composable
internal fun recapItemsText(items: StatisticsRecapItems): String = when (items) {
    is StatisticsRecapItems.Single -> stringResource(items.label, formatChapterNumber(items.number))
    is StatisticsRecapItems.Range -> stringResource(
        items.label,
        formatChapterNumber(items.first),
        formatChapterNumber(items.last),
    )
}

@Composable
internal fun recapConsumedText(count: StatisticsRecapConsumedCount): String =
    pluralStringResource(count.plural, count.count.toInt(), count.count.toInt())

@Composable
internal fun recapTypeName(type: EntryType): String = stringResource(type.entryTypePresentation().displayNameLabel)

@Composable
internal fun recapComparedPeriodText(period: ComparedPeriod, formats: RecapFormats): String = when (period) {
    is ComparedPeriod.Year -> period.year.toString()
    is ComparedPeriod.Month -> formats.month(period.month)
}

internal const val HOUR_MILLIS = 3_600_000L
internal const val MINUTE_MILLIS = 60_000L
