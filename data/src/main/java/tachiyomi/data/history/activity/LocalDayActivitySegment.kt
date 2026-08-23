package tachiyomi.data.history.activity

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

internal data class LocalDayActivitySegment(
    val localDate: String,
    val startedAtEpochMillis: Long,
    val endedAtEpochMillis: Long,
    val durationMillis: Long,
)

/** Splits a contiguous checkpoint at experienced local-midnight boundaries while preserving its exact duration. */
internal fun splitActivityByLocalDay(
    startedAtEpochMillis: Long,
    endedAtEpochMillis: Long,
    durationMillis: Long,
    timeZoneId: String,
): List<LocalDayActivitySegment> {
    if (durationMillis <= 0L) return emptyList()

    val timeZone = TimeZone.of(timeZoneId)
    val elapsedMillis = (endedAtEpochMillis - startedAtEpochMillis).coerceAtLeast(1L)
    val boundaries = buildList {
        var cursor = startedAtEpochMillis
        while (cursor < endedAtEpochMillis) {
            val cursorInstant = Instant.fromEpochMilliseconds(cursor)
            val localDate = cursorInstant.toLocalDateTime(timeZone).date
            val nextMidnight = localDate.plus(1, DateTimeUnit.DAY).atStartOfDayIn(timeZone).toEpochMilliseconds()
            val segmentEnd = minOf(endedAtEpochMillis, nextMidnight.coerceAtLeast(cursor + 1L))
            add(Triple(localDate.toString(), cursor, segmentEnd))
            cursor = segmentEnd
        }
    }

    var allocatedDuration = 0L
    return boundaries.mapIndexed { index, (localDate, start, end) ->
        val duration = if (index == boundaries.lastIndex) {
            durationMillis - allocatedDuration
        } else {
            ((durationMillis.toDouble() * (end - start).toDouble()) / elapsedMillis.toDouble())
                .toLong()
                .coerceAtLeast(0L)
                .also { allocatedDuration += it }
        }
        LocalDayActivitySegment(
            localDate = localDate,
            startedAtEpochMillis = start,
            endedAtEpochMillis = end,
            durationMillis = duration,
        )
    }.filter { it.durationMillis > 0L }
}
