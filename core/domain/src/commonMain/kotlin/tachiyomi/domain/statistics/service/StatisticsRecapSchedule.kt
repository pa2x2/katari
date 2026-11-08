package tachiyomi.domain.statistics.service

import app.katari.core.common.platform.PlatformFacility

/** The daily check that posts the monthly and yearly statistics recaps once they are due. */
@PlatformFacility
interface StatisticsRecapSchedule {
    /** Matches the schedule to the active profile's settings. */
    suspend fun setupTask()
}
