package eu.kanade.presentation.more.stats.components

import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

internal fun formatStatisticsWindow(window: StatsActivityWindow): String {
    val formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())
    val start = window.startDate
    return when {
        start == null || start == window.endDate -> window.endDate.format(formatter)
        else -> "${start.format(formatter)} – ${window.endDate.format(formatter)}"
    }
}
