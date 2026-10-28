package eu.kanade.presentation.more.stats.components

import com.ibm.icu.text.DateFormat
import com.ibm.icu.text.DateIntervalFormat
import com.ibm.icu.util.DateInterval
import mihon.core.platform.PlatformContext
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

// Android's DateUtils formats these through ICU with the same skeletons.
internal actual fun PlatformContext.formatStatisticsDay(date: LocalDate, withYear: Boolean): String =
    DateFormat.getInstanceForSkeleton(skeleton(withYear), Locale.getDefault()).format(Date(date.startMillis()))

internal actual fun PlatformContext.formatStatisticsDays(
    start: LocalDate,
    endInclusive: LocalDate,
    withYear: Boolean,
): String = DateIntervalFormat.getInstance(skeleton(withYear), Locale.getDefault())
    .format(DateInterval(start.startMillis(), endInclusive.startMillis()))

private fun skeleton(withYear: Boolean): String = if (withYear) "yMMMd" else "MMMd"

private fun LocalDate.startMillis(): Long = atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
