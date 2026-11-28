package tachiyomi.domain.source.lnreader

import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull
import java.text.ParsePosition
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.format.DateTimeFormatterBuilder
import java.time.temporal.TemporalAccessor
import java.time.temporal.TemporalQueries
import java.util.Locale

/**
 * A chapter's `releaseTime` as a time in epoch milliseconds, or else the site's text to show as it is.
 *
 * Plugins send ISO 8601 times, epoch numbers, the site's own wording, or what dayjs formats: the runtime's dayjs is
 * LNReader's in English, so `format('LLL')` gives "October 26, 2026 11:20 PM". A format dayjs left unexpanded ("LLL"),
 * "Invalid Date" and blanks say nothing.
 */
class LnReaderReleaseTime(val date: Long, val text: String?)

fun lnReaderReleaseTime(value: JsonElement?, zone: ZoneId = ZoneId.systemDefault()): LnReaderReleaseTime? {
    val primitive = value as? JsonPrimitive ?: return null
    if (!primitive.isString) return primitive.doubleOrNull?.let { epoch(it.toLong()) }
    val text = primitive.content.trim().takeIf { it.isNotEmpty() && it !in NOTHING } ?: return null
    if (text.all(Char::isDigit)) text.toLongOrNull()?.let { return epoch(it) }
    return parse(text, zone)?.let { LnReaderReleaseTime(it, null) } ?: LnReaderReleaseTime(0L, text)
}

// Seconds until the year 5138, milliseconds after 1973
private fun epoch(value: Long) = LnReaderReleaseTime(if (value < 100_000_000_000L) value * 1000 else value, null)

private fun parse(text: String, zone: ZoneId): Long? {
    ISO_OFFSET_DATE_TIME.parseWhole(text)?.let { return OffsetDateTime.from(it).toInstant().toEpochMilli() }
    ISO_INSTANT.parseWhole(text)?.let { return Instant.from(it).toEpochMilli() }
    ISO_COMPACT_OFFSET.parseWhole(text)?.let { return OffsetDateTime.from(it).toInstant().toEpochMilli() }
    ISO_LOCAL_DATE_TIME.parseWhole(text.replaceFirst(' ', 'T'))?.let {
        return LocalDateTime.from(it).atZone(zone).toInstant().toEpochMilli()
    }
    ISO_LOCAL_DATE.parseWhole(text)?.let { return LocalDate.from(it).atStartOfDay(zone).toInstant().toEpochMilli() }
    for (format in ENGLISH_DATES) {
        format.parseWhole(text)?.let { return it.toEpochMilli(zone) }
    }
    return null
}

// parse() throws for every format a text doesn't fit, and a long chapter list tries several per chapter (D112).
// Resolving still throws where the text's fields disagree, such as a weekday that isn't the date's
private fun DateTimeFormatter.parseWhole(text: String): TemporalAccessor? {
    val position = ParsePosition(0)
    parseUnresolved(text, position) ?: return null
    if (position.errorIndex >= 0 || position.index != text.length) return null
    return runCatching { parse(text) }.getOrNull()
}

private fun TemporalAccessor.toEpochMilli(zone: ZoneId): Long {
    val date = LocalDate.from(this)
    val time = query(TemporalQueries.localTime()) ?: LocalTime.MIDNIGHT
    return LocalDateTime.of(date, time).atZone(zone).toInstant().toEpochMilli()
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
