package app.katari.android.time

import android.text.format.DateFormat
import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.SystemClockFormat

/** The clock the user set in Android's settings, or else the one of the screen's language. */
@Inject
class DateFormatSystemClockFormat : SystemClockFormat {
    override fun is24Hour(context: PlatformContext): Boolean = DateFormat.is24HourFormat(context)
}
