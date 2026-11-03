package eu.kanade.presentation.library.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLocale
import eu.kanade.domain.ui.bestDateTimePattern
import eu.kanade.presentation.util.rememberHourSkeleton
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

/** "Oct 10, 7:30 PM": when a source's pause ends, with the year only when it isn't this year's. */
@Composable
fun sourcePauseEndText(until: Long): String {
    val locale = LocalLocale.current.platformLocale
    val hourSkeleton = rememberHourSkeleton()
    val end = Instant.ofEpochMilli(until).atZone(ZoneId.systemDefault())
    val date = if (end.year == ZonedDateTime.now().year) "MMMd" else "yMMMd"
    val formatter = remember(locale, date, hourSkeleton) {
        DateTimeFormatter.ofPattern(bestDateTimePattern(locale, "$date${hourSkeleton}mm"), locale)
    }
    return formatter.format(end)
}

/** "Paused", or "Paused until Oct 10, 7:30 PM" for a pause that ends on its own. */
@Composable
fun sourcePauseStateText(until: Long?): String {
    return if (until == null) {
        stringResource(MR.strings.library_update_source_paused)
    } else {
        stringResource(MR.strings.library_update_source_paused_until, sourcePauseEndText(until))
    }
}
