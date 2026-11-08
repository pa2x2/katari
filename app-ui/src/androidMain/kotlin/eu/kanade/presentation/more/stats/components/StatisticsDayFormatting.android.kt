package eu.kanade.presentation.more.stats.components

import android.text.format.DateUtils
import app.katari.core.platform.PlatformContext
import java.time.LocalDate
import java.time.ZoneId

internal actual fun PlatformContext.formatStatisticsDay(date: LocalDate, withYear: Boolean): String =
    DateUtils.formatDateTime(this, date.startMillis(), flags(withYear))

// The range ends at the midnight after its last day, which DateUtils reads as that day's end.
internal actual fun PlatformContext.formatStatisticsDays(
    start: LocalDate,
    endInclusive: LocalDate,
    withYear: Boolean,
): String = DateUtils.formatDateRange(
    this,
    start.startMillis(),
    endInclusive.plusDays(1L).startMillis(),
    flags(withYear),
)

private fun flags(withYear: Boolean): Int {
    val flags = DateUtils.FORMAT_SHOW_DATE or DateUtils.FORMAT_ABBREV_MONTH
    return if (withYear) flags else flags or DateUtils.FORMAT_NO_YEAR
}

private fun LocalDate.startMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
