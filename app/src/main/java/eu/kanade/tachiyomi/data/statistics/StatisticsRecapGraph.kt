package eu.kanade.tachiyomi.data.statistics

import eu.kanade.tachiyomi.ui.stats.recap.delivery.StatisticsRecapDue
import tachiyomi.data.ActiveProfileProvider

/** Graph entry points of the recap notification job, which WorkManager constructs. */
interface StatisticsRecapGraph {
    val activeProfileProvider: ActiveProfileProvider
    val statisticsRecapDue: StatisticsRecapDue
}
