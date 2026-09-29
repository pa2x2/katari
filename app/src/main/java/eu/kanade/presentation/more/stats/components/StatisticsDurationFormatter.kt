package eu.kanade.presentation.more.stats.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.*

/** Smallest unit shown by Statistics durations; sub-minute activity reads as "<1 min" at minute precision. */
internal enum class StatisticsDurationPrecision {
    MINUTES,
    SECONDS,
}

/** Localized unit renderers, so formatting stays independent of Android resources. */
internal class StatisticsDurationUnits(
    val hours: (Long) -> String,
    val minutes: (Long) -> String,
    val seconds: (Long) -> String,
    val lessThanMinute: String,
)

internal fun formatStatisticsDuration(
    durationMillis: Long,
    precision: StatisticsDurationPrecision,
    units: StatisticsDurationUnits,
): String {
    val totalSeconds = durationMillis.coerceAtLeast(0L) / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = totalSeconds / 60L % 60L
    val seconds = totalSeconds % 60L
    return when {
        hours > 0L -> listOfNotNull(units.hours(hours), units.minutes(minutes).takeIf { minutes > 0L })
            .joinToString(" ")
        precision == StatisticsDurationPrecision.MINUTES && durationMillis in 1L..59_999L -> units.lessThanMinute
        precision == StatisticsDurationPrecision.MINUTES -> units.minutes(minutes)
        minutes > 0L -> listOfNotNull(units.minutes(minutes), units.seconds(seconds).takeIf { seconds > 0L })
            .joinToString(" ")
        else -> units.seconds(seconds)
    }
}

@Composable
internal fun rememberStatisticsDurationFormatter(
    precision: StatisticsDurationPrecision = StatisticsDurationPrecision.MINUTES,
): (Long) -> String {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    return remember(context, locale, precision) {
        val units = StatisticsDurationUnits(
            hours = { context.stringResource(MR.strings.hour_short, it) },
            minutes = { context.stringResource(MR.strings.minute_short, it) },
            seconds = { context.stringResource(MR.strings.seconds_short, it) },
            lessThanMinute = context.stringResource(MR.strings.statistics_less_than_minute),
        )
        val formatter: (Long) -> String = { formatStatisticsDuration(it, precision, units) }
        formatter
    }
}
