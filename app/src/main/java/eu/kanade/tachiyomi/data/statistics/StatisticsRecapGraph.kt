package eu.kanade.tachiyomi.data.statistics

import mihon.feature.profiles.core.ProfileStore
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.statistics.recap.StatisticsRecapRepository

/** Graph entry points of the recap notification job, which WorkManager constructs, and of its scheduling. */
interface StatisticsRecapGraph {
    val activeProfileProvider: ActiveProfileProvider
    val profileStore: ProfileStore
    val statisticsRecapRepository: StatisticsRecapRepository
}
