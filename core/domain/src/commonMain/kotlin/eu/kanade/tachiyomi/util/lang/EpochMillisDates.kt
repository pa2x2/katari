package eu.kanade.tachiyomi.util.lang

import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

fun Long.convertEpochMillisZone(
    from: TimeZone,
    to: TimeZone,
): Long {
    return Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(from)
        .toInstant(to)
        .toEpochMilliseconds()
}

fun Long.toLocalDate(): LocalDate {
    return Instant.fromEpochMilliseconds(this).toLocalDateTime(TimeZone.currentSystemDefault()).date
}

fun Long.toJavaLocalDate(): java.time.LocalDate {
    return this.toLocalDate().toJavaLocalDate()
}
