package eu.kanade.tachiyomi.util.lang

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.toJavaLocalDateTime
import java.time.format.DateTimeFormatter

fun LocalDateTime.toDateTimestampString(dateTimeFormatter: DateTimeFormatter): String {
    return dateTimeFormatter.format(this.toJavaLocalDateTime())
}
