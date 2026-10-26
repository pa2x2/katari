package eu.kanade.tachiyomi.source.lnreader.entry

import eu.kanade.tachiyomi.source.lnreader.runtime.asNumber
import eu.kanade.tachiyomi.source.lnreader.runtime.asText
import kotlinx.serialization.json.JsonElement
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.TemporalAccessor
import java.util.Locale

/**
 * A chapter's `releaseTime` as a time in epoch milliseconds, or else the site's text to show as it is.
 *
 * Plugins send ISO 8601 times, epoch numbers, the site's own wording, or what dayjs formats: the runtime's dayjs is
 * LNReader's in English, so `format('LLL')` gives "October 26, 2026 11:20 PM". A format dayjs left unexpanded ("LLL"),
 * "Invalid Date" and blanks say nothing.
 */
internal class LnReaderReleaseTime(val date: Long, val text: String?)

internal fun lnReaderReleaseTime(value: JsonElement?, zone: ZoneId = ZoneId.systemDefault()): LnReaderReleaseTime? {
    value.asNumber()?.let { return epoch(it.toLong()) }
    val text = value.asText()?.trim()?.takeIf { it.isNotEmpty() && it !in NOTHING } ?: return null
    if (text.all(Char::isDigit)) text.toLongOrNull()?.let { return epoch(it) }
    return parse(text, zone)?.let { LnReaderReleaseTime(it, null) } ?: LnReaderReleaseTime(0L, text)
}

// Seconds until the year 5138, milliseconds after 1973
private fun epoch(value: Long) = LnReaderReleaseTime(if (value < 100_000_000_000L) value * 1000 else value, null)

private fun parse(text: String, zone: ZoneId): Long? {
    runCatching { return OffsetDateTime.parse(text).toInstant().toEpochMilli() }
    runCatching { return Instant.parse(text).toEpochMilli() }
    runCatching { return OffsetDateTime.parse(text, ISO_COMPACT_OFFSET).toInstant().toEpochMilli() }
    val isoLocal = text.replaceFirst(' ', 'T')
    runCatching { return LocalDateTime.parse(isoLocal).atZone(zone).toInstant().toEpochMilli() }
    runCatching { return LocalDate.parse(text).atStartOfDay(zone).toInstant().toEpochMilli() }
    for (format in ENGLISH_DATES) {
        val parsed = runCatching { format.parse(text) }.getOrNull() ?: continue
        return parsed.toEpochMilli(zone)
    }
    return null
}

private fun TemporalAccessor.toEpochMilli(zone: ZoneId): Long {
    val date = LocalDate.from(this)
    return runCatching { LocalDateTime.of(date, LocalTime.from(this)) }
        .getOrElse { date.atStartOfDay() }
        .atZone(zone)
        .toInstant()
        .toEpochMilli()
}

// Java's and many APIs' ISO form, whose offset has no colon: 2025-01-29T15:10:03.000+0000
private val ISO_COMPACT_OFFSET: DateTimeFormatter = DateTimeFormatterBuilder()
    .append(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
    .appendOffset("+HHMM", "Z")
    .toFormatter(Locale.ROOT)

private val NOTHING = setOf("L", "LL", "LLL", "LLLL", "l", "ll", "lll", "llll", "LT", "LTS", "Invalid Date")

// dayjs's English LLLL, LLL, LL, the short month forms sites use alike, and JavaScript's Date.toDateString()
private val ENGLISH_DATES: List<DateTimeFormatter> = listOf(
    "EEEE, MMMM d, yyyy h:mm a",
    "MMMM d, yyyy h:mm a",
    "MMMM d, yyyy",
    "MMM d, yyyy h:mm a",
    "MMM d, yyyy",
    "d MMMM yyyy",
    "d MMM yyyy",
    "EEE MMM d yyyy",
).map { pattern ->
    DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.ENGLISH)
}
