package tachiyomi.domain.library.service

import kotlinx.coroutines.flow.Flow
import mihon.core.common.platform.PlatformFacility

/**
 * Automatic library updates, one schedule per profile, at the profile's own interval and device restrictions. A due
 * schedule only adds the profile's update to the [LibraryUpdateQueue], so profiles that are due at the same time update
 * one after another.
 */
@PlatformFacility
interface LibraryUpdateSchedule {
    /**
     * Whether automatic updates can wait for the device restrictions the user picks, such as Wi-Fi or charging; the
     * setting is hidden where they can't.
     */
    val offersDeviceRestrictions: Boolean

    /**
     * Schedules automatic updates of [profileId] at the shortest interval its library or any category asks for, or
     * cancels them when nothing is updated automatically or the profile is archived.
     *
     * @param prefInterval the library interval when its preference is about to change and doesn't hold it yet.
     */
    suspend fun schedule(profileId: Long, prefInterval: Int? = null)

    /** [schedule] for the active profile, after a screen changed its settings. */
    suspend fun scheduleActiveProfile(prefInterval: Int? = null)

    /** Brings every profile's schedule in line with its settings, and drops the schedule older versions shared. */
    suspend fun scheduleAll()

    fun cancel(profileId: Long)

    /** When the next automatic update of [profileId] is due to start, or null when none is scheduled. */
    fun nextRunFlow(profileId: Long): Flow<Long?>
}
