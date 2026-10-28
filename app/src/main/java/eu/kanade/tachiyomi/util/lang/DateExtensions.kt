package eu.kanade.tachiyomi.util.lang

import android.content.Context
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.daysUntil
import kotlinx.datetime.toJavaLocalDate
import kotlinx.datetime.toLocalDateTime
import tachiyomi.i18n.*
import tachiyomi.i18n.context.pluralStringResource
import tachiyomi.i18n.context.stringResource
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import kotlin.math.absoluteValue
import kotlin.time.Clock

fun Date.toTimestampString(timeFormatter: DateTimeFormatter): String {
    return timeFormatter.format(toInstant().atZone(ZoneId.systemDefault()))
}

fun LocalDate.toRelativeString(
    context: Context,
    relative: Boolean = true,
    dateFormat: DateTimeFormatter,
): String {
    if (!relative) {
        return dateFormat.format(this.toJavaLocalDate())
    }
    val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    val difference = this.daysUntil(today)
    return when {
        difference < -7 -> dateFormat.format(this.toJavaLocalDate())
        difference < 0 -> context.pluralStringResource(
            MR.plurals.upcoming_relative_time,
            difference.absoluteValue,
            difference.absoluteValue,
        )

        difference < 1 -> context.stringResource(MR.strings.relative_time_today)
        difference < 7 -> context.pluralStringResource(
            MR.plurals.relative_time,
            difference,
            difference,
        )

        else -> dateFormat.format(this.toJavaLocalDate())
    }
}
