package eu.kanade.tachiyomi.ui.home

import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.stats.recap.delivery.StatisticsUnopenedYearRecap
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.domain.library.service.LibraryPreferences

/** Graph services the home screen and its tab badges read; the home screen has no view model of its own. */
interface HomeGraph {
    val profileManager: ProfileManager
    val profileStore: ProfileStore
    val uiPreferences: UiPreferences
    val libraryPreferences: LibraryPreferences
    val extensionManager: ExtensionManager
    val statisticsUnopenedYearRecap: StatisticsUnopenedYearRecap
}
