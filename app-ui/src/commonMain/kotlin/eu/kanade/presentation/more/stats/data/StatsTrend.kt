package eu.kanade.presentation.more.stats.data

import eu.kanade.tachiyomi.source.entry.EntryType
import java.time.LocalDate

data class StatsTrendPoint(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val durationByType: Map<EntryType, Long>,
    val completionCountByType: Map<EntryType, Long> = emptyMap(),
    val trackedStartDate: LocalDate? = startDate,
    val bucketStartDate: LocalDate = startDate,
) {
    val totalDurationMillis: Long = durationByType.values.sum()
    val completionCount: Long = completionCountByType.values.sum()
    val isTracked: Boolean = trackedStartDate != null
}

enum class StatsTrendGranularity {
    DAY,
    WEEK,
    MONTH,
    YEAR,
}
