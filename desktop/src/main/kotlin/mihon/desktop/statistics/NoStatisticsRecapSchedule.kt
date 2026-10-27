package mihon.desktop.statistics

import dev.zacsweers.metro.Inject
import tachiyomi.domain.statistics.service.StatisticsRecapSchedule

/**
 * Desktop runs no background work and posts no notifications yet, so the statistics recaps are never posted. Stands
 * in until desktop can schedule the daily check.
 */
@Inject
class NoStatisticsRecapSchedule : StatisticsRecapSchedule {
    override suspend fun setupTask() = Unit
}
