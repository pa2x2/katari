package tachiyomi.core.metadata.comicinfo

import java.time.DateTimeException
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * ComicInfo dates a chapter with separate Year, Month and Day fields. Downloads write the calendar day of the
 * chapter's upload date and the local source reads it back as the start of that day, both in the device time
 * zone that chapter lists show upload dates in, so a downloaded chapter moved to the local source keeps its day.
 */
data class ComicInfoDate(
    val year: ComicInfo.Year,
    val month: ComicInfo.Month,
    val day: ComicInfo.Day,
)

/** Null when the chapter has no upload date (zero or negative). */
fun comicInfoDateOf(uploadDateMillis: Long, zone: ZoneId = ZoneId.systemDefault()): ComicInfoDate? {
    if (uploadDateMillis <= 0) return null
    val date = Instant.ofEpochMilli(uploadDateMillis).atZone(zone).toLocalDate()
    return ComicInfoDate(
        year = ComicInfo.Year(date.year),
        month = ComicInfo.Month(date.monthValue),
        day = ComicInfo.Day(date.dayOfMonth),
    )
}

/**
 * A missing month or day means the first; out-of-range values are clamped. Null when the year is missing or the
 * fields don't name a calendar day, such as February 30.
 */
fun ComicInfo.uploadDateMillis(zone: ZoneId = ZoneId.systemDefault()): Long? {
    val year = year?.value?.takeIf { it > 0 } ?: return null
    val month = month?.value?.coerceIn(1, 12) ?: 1
    val day = day?.value?.coerceIn(1, 31) ?: 1
    return try {
        LocalDate.of(year, month, day).atStartOfDay(zone).toInstant().toEpochMilli()
    } catch (_: DateTimeException) {
        null
    }
}
