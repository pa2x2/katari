package mihon.desktop.home

import eu.kanade.tachiyomi.ui.stats.recap.period.StatisticsRecapPeriod
import mihon.feature.migration.session.model.SourceMigrationSessionId

/**
 * A place in the window's home that something outside the window opens, such as a notification, as Android's
 * notifications open the main activity with an intent. Those of a profile open in it, switching to it first.
 */
sealed interface HomeDestination {
    data class Library(val profileId: Long) : HomeDestination

    data class Updates(val profileId: Long) : HomeDestination

    data class LibraryUpdateReport(val profileId: Long) : HomeDestination

    data class LibraryUpdateSettings(val profileId: Long) : HomeDestination

    data class StatisticsRecap(val profileId: Long, val period: StatisticsRecapPeriod) : HomeDestination

    /** The review of a migration session, in the session's profile, while the session hasn't ended. */
    data class SourceMigrationReview(val sessionId: SourceMigrationSessionId) : HomeDestination

    /** The extensions page, in the active profile: extensions are installed for every profile. */
    data object Extensions : HomeDestination
}
