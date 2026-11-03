package mihon.app.time

import android.text.format.DateFormat
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.SystemClockFormat
import mihon.core.platform.PlatformContext

/** The clock the user set in Android's settings, or else the one of the screen's language. */
@Inject
class DateFormatSystemClockFormat : SystemClockFormat {
    override fun is24Hour(context: PlatformContext): Boolean = DateFormat.is24HourFormat(context)
}
