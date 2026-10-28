package eu.kanade.presentation.more.stats.data

import eu.kanade.tachiyomi.source.entry.EntryType
import java.time.LocalDate

/** Daily timed activity for the reading calendar, always covering the last twelve months up to today. */
data class StatsReadingCalendar(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val durationByDate: Map<LocalDate, Map<EntryType, Long>>,
) {
    fun durationOn(date: LocalDate, type: EntryType?): Long {
        val day = durationByDate[date] ?: return 0L
        return if (type == null) day.values.sum() else day[type] ?: 0L
    }
}
