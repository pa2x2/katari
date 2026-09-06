package eu.kanade.presentation.more.stats.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource
import java.text.NumberFormat

@Composable
internal fun rememberStatisticsDurationFormatter(): (Long) -> String {
    val locale = LocalConfiguration.current.locales[0]
    val shortDuration = stringResource(MR.strings.statistics_less_than_minute)
    return remember(locale, shortDuration) {
        val numbers = NumberFormat.getIntegerInstance(locale)
        val formatter: (Long) -> String = { durationMillis ->
            val totalMinutes = durationMillis.coerceAtLeast(0L) / 60_000L
            val hours = totalMinutes / 60L
            val minutes = totalMinutes % 60L
            when {
                durationMillis in 1L..59_999L -> shortDuration
                hours > 0L && minutes > 0L -> "${numbers.format(hours)}h ${numbers.format(minutes)}m"
                hours > 0L -> "${numbers.format(hours)}h"
                else -> "${numbers.format(minutes)}m"
            }
        }
        formatter
    }
}
