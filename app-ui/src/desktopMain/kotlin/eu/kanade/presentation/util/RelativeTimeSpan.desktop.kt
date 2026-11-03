package eu.kanade.presentation.util

import com.ibm.icu.text.DisplayContext
import com.ibm.icu.text.RelativeDateTimeFormatter
import com.ibm.icu.text.RelativeDateTimeFormatter.AbsoluteUnit
import com.ibm.icu.text.RelativeDateTimeFormatter.Direction
import com.ibm.icu.text.RelativeDateTimeFormatter.RelativeUnit
import com.ibm.icu.text.RelativeDateTimeFormatter.Style
import com.ibm.icu.util.ULocale
import eu.kanade.domain.ui.bestDateTimePattern
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds

// What Android's DateUtils.getRelativeTimeSpanString writes at minute resolution, from the ICU it is built on: days
// count calendar days, so the evening before is "yesterday", and a week or more away is the date with the year.
internal actual fun relativeTimeSpan(epochMillis: Long, now: Long): String {
    val locale = Locale.getDefault(Locale.Category.FORMAT)
    val formatter = RelativeDateTimeFormatter.getInstance(
        ULocale.forLocale(locale),
        null,
        Style.LONG,
        DisplayContext.CAPITALIZATION_FOR_BEGINNING_OF_SENTENCE,
    )
    val span = (epochMillis - now).milliseconds
    val direction = if (span.isNegative()) Direction.LAST else Direction.NEXT
    val zone = ZoneId.systemDefault()
    val time = Instant.ofEpochMilli(epochMillis).atZone(zone)
    return when {
        span.absoluteValue < 1.hours ->
            formatter.format(span.absoluteValue.inWholeMinutes.toDouble(), direction, RelativeUnit.MINUTES)
        span.absoluteValue < 1.days ->
            formatter.format(span.absoluteValue.inWholeHours.toDouble(), direction, RelativeUnit.HOURS)
        else -> {
            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            val days = abs(ChronoUnit.DAYS.between(today, time.toLocalDate()))
            when {
                days == 1L -> formatter.format(direction, AbsoluteUnit.DAY)
                days < 7L -> formatter.format(days.toDouble(), direction, RelativeUnit.DAYS)
                else -> DateTimeFormatter.ofPattern(bestDateTimePattern(locale, "yMMMd"), locale).format(time)
            }
        }
    }
}
