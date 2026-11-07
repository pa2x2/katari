package app.katari.sync.client.trigger

import mihon.core.common.platform.PlatformFacility
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
     * Runs [SyncBackgroundRun.firstDownload] as work that shows its progress and keeps going when the app leaves the
     * front, on any network when [onMetered], otherwise on an unmetered one. A platform whose runs end with the app,
     * such as the desktop, leaves the first download to them and shows its progress only in the app.
     */
    fun firstDownload(onMetered: Boolean)

    /** False while battery saver or a background restriction is on, which pauses background sync (D022). */
    fun allowsBackgroundSync(): Boolean

    companion object {
        val PERIOD: Duration = 1.hours
    }
}
