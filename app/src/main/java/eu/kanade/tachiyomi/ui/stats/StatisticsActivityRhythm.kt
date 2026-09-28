package eu.kanade.tachiyomi.ui.stats

import eu.kanade.presentation.more.stats.data.StatsActivityRhythm
import tachiyomi.domain.statistics.model.StatisticsActivityBucket
import tachiyomi.domain.statistics.model.StatisticsActivitySegment
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Spreads each segment's active time over the local clock hours its wall-clock span covers, in the
 * zone it was recorded in. Weekdays come from the recorded local dates, which already follow that zone.
 */
internal fun buildActivityRhythm(
    segments: List<StatisticsActivitySegment>,
    activity: List<StatisticsActivityBucket>,
): StatsActivityRhythm {
    val hourly = LongArray(24)
    segments.forEach { segment -> segment.addHoursTo(hourly) }
    val weekdays = DayOfWeek.entries.associateWith { 0L }.toMutableMap()
    activity.forEach { bucket ->
        val day = LocalDate.parse(bucket.localDate).dayOfWeek
        weekdays[day] = weekdays.getValue(day) + bucket.durationMillis
    }
    return StatsActivityRhythm(hourlyDurationMillis = hourly.toList(), weekdayDurationMillis = weekdays)
}

private fun StatisticsActivitySegment.addHoursTo(hourly: LongArray) {
    if (durationMillis <= 0L) return
    val zone = runCatching { ZoneId.of(timeZoneId) }.getOrDefault(ZoneId.systemDefault())
    val start = Instant.ofEpochMilli(startedAtEpochMillis).atZone(zone)
    val wallMillis = endedAtEpochMillis - startedAtEpochMillis
    if (wallMillis <= 0L) {
        hourly[start.hour] += durationMillis
        return
    }
    var assigned = 0L
    var cursor = start
    val end = Instant.ofEpochMilli(endedAtEpochMillis).atZone(zone)
    while (cursor.isBefore(end)) {
        val nextHour = cursor.truncatedTo(ChronoUnit.HOURS).plusHours(1L).let { if (it.isAfter(end)) end else it }
        val sliceMillis = ChronoUnit.MILLIS.between(cursor, nextHour)
        val share = if (!nextHour.isBefore(end)) {
            durationMillis - assigned
        } else {
            durationMillis * sliceMillis / wallMillis
        }
        hourly[cursor.hour] += share
        assigned += share
        cursor = nextHour
    }
}
