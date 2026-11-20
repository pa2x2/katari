package app.katari.ui.automaticcheck

import app.katari.core.common.platform.PlatformFacility

/** A clock that only moves forward and keeps counting while the device sleeps, for spacing [AutomaticChecks]. */
@PlatformFacility
interface SleepCountingClock {
    fun elapsedMillis(): Long
}
