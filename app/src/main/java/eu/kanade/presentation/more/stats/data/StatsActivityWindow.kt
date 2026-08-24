package eu.kanade.presentation.more.stats.data

import java.time.LocalDate

data class StatsActivityWindow(
    val range: StatsRange,
    val startDate: LocalDate?,
    val endDate: LocalDate,
    val isLatest: Boolean,
)
