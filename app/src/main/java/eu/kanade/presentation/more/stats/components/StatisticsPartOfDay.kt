package eu.kanade.presentation.more.stats.components

import dev.icerock.moko.resources.StringResource
import tachiyomi.i18n.*

/** Coarse time of day used to describe when someone usually reads or watches. */
internal enum class StatisticsPartOfDay(val mostlyLabel: StringResource) {
    MORNING(MR.strings.statistics_mostly_mornings),
    AFTERNOON(MR.strings.statistics_mostly_afternoons),
    EVENING(MR.strings.statistics_mostly_evenings),
    NIGHT(MR.strings.statistics_mostly_nights),
    ;

    companion object {
        fun of(hour: Int): StatisticsPartOfDay = when (hour) {
            in 5..11 -> MORNING
            in 12..16 -> AFTERNOON
            in 17..21 -> EVENING
            else -> NIGHT
        }
    }
}

/** The part of day with the most time, summing its hours; night spans midnight. */
internal fun dominantPartOfDay(hourlyDurationMillis: List<Long>): StatisticsPartOfDay? = hourlyDurationMillis
    .withIndex()
    .filter { it.value > 0L }
    .groupBy({ StatisticsPartOfDay.of(it.index) }, { it.value })
    .maxByOrNull { (_, durations) -> durations.sum() }
    ?.key

internal fun peakHour(hourlyDurationMillis: List<Long>): Int? = hourlyDurationMillis.indices
    .filter { hourlyDurationMillis[it] > 0L }
    .maxByOrNull { hourlyDurationMillis[it] }
