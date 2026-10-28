package eu.kanade.presentation.more.stats.data

import java.time.LocalDate

data class StatsActivityWindow(
    val range: StatsRange,
    val startDate: LocalDate?,
    val endDate: LocalDate,
    val isLatest: Boolean,
    // Retain the intended day when calendar-month navigation crosses a shorter month.
    val monthAnchorDay: Int = endDate.dayOfMonth,
)
