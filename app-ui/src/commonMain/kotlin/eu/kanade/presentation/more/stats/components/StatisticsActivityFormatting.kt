package eu.kanade.presentation.more.stats.components

import app.katari.core.platform.PlatformContext
import eu.kanade.presentation.more.stats.data.StatsActivityWindow
import eu.kanade.presentation.more.stats.data.StatsRange
import java.time.LocalDate

/** A window such as "Nov 16 – Dec 15", with the year for a year's window or one that isn't this year. */
internal fun PlatformContext.formatStatisticsWindow(window: StatsActivityWindow): String {
    val start = window.startDate ?: window.endDate
    val withYear = window.range == StatsRange.ONE_YEAR ||
        start.year != LocalDate.now().year ||
        window.endDate.year != LocalDate.now().year
    return if (start == window.endDate) {
        formatStatisticsDay(start, withYear)
    } else {
        formatStatisticsDays(start, window.endDate, withYear)
    }
}
