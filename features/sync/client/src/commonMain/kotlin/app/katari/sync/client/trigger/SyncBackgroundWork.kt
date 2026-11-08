package app.katari.sync.client.trigger

import app.katari.core.common.platform.PlatformFacility
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours

/**
 * How the platform runs sync when no screen asks for it (D021, D022): Android's WorkManager, which also runs while
 * the app is closed, and on desktop the app's in-process work while it is open. Each platform's work calls
 * [SyncBackgroundRun], which holds what a run in the background does.
 */
@PlatformFacility
interface SyncBackgroundWork {
    /** Runs [SyncBackgroundRun.periodic] about every [PERIOD] while [enabled], or stops doing so. */
    suspend fun schedulePeriodic(enabled: Boolean)

    /**
     * Runs [SyncBackgroundRun.handOff] as soon as there is a network, as work the platform lets finish when the app
     * leaves the foreground and runs again until it succeeds: what was read or watched just now goes to the account
     * even when the app is closed right after.
     */
    fun handOff()

    /**
     * Runs [SyncBackgroundRun.longRun] as work that shows its progress in a notification and keeps going when the app
     * leaves the front, on any network when [onMetered], otherwise on an unmetered one. Work for it already waiting or
     * going carries on instead.
     */
    fun longRun(onMetered: Boolean)

    /**
     * Drops the long run's work, waiting or retrying, as the device leaves its account: left waiting, it would carry on
     * in place of the next account's long run, at its own retry's time.
     */
    fun dropLongRun()

    /** False while battery saver or a background restriction is on, which pauses background sync (D022). */
    fun allowsBackgroundSync(): Boolean

    companion object {
        val PERIOD: Duration = 1.hours
    }
}
