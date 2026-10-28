package eu.kanade.tachiyomi.ui.home.navigation

import eu.kanade.tachiyomi.extension.PendingExtensionUpdates
import eu.kanade.tachiyomi.ui.stats.recap.delivery.StatisticsUnopenedYearRecap

/** Graph entry points of the home tabs' badges. */
interface HomeNavigationGraph {
    val pendingExtensionUpdates: PendingExtensionUpdates
    val statisticsUnopenedYearRecap: StatisticsUnopenedYearRecap
}
