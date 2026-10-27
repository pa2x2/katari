package tachiyomi.domain.statistics.service

/** The daily check that posts the monthly and yearly statistics recaps once they are due. */
interface StatisticsRecapSchedule {
    /** Matches the schedule to the active profile's settings. */
    suspend fun setupTask()
}
