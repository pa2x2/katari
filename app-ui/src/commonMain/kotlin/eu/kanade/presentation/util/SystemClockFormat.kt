package eu.kanade.presentation.util

import mihon.core.common.platform.PlatformFacility
import mihon.core.platform.PlatformContext

/** The system's 12/24-hour clock setting, which the Time format setting's "Follow system" choice reads. */
@PlatformFacility
interface SystemClockFormat {
    /**
     * Whether the system shows times on the 24-hour clock, as [context]'s screen would, whose language some systems
     * take the clock from. Read again each time the app resumes, since the setting can change in the background.
     */
    fun is24Hour(context: PlatformContext): Boolean
}
