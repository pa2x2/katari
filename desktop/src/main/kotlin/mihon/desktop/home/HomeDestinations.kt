package mihon.desktop.home

import cafe.adriel.voyager.navigator.Navigator
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportScreen
import eu.kanade.tachiyomi.ui.main.link.openAppLink
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.stats.recap.StatisticsRecapScreen
import logcat.LogPriority
import mihon.desktop.window.OpenWindow
import mihon.feature.migration.review.SourceMigrationReviewScreen
import mihon.feature.migration.session.SourceMigrationSessionStore
import mihon.feature.profiles.core.ProfileManager
import tachiyomi.core.common.util.system.logcat

/**
 * Opens [HomeDestination]s in the window's home, as Android's main activity carries out its notifications' and links'
 * intents. A destination in another profile switches to it first and opens once the home has started for it; one in
 * whichever profile the home shows opens in the profile the home starts with, picked at start or switched to. One
 * that comes before the home has started, or while it starts again for a profile switch, waits for it, and only the
 * last one waits, as Android keeps only the last intent. Desktop can't unlock a locked profile, so nothing opens in
 * one. The navigator is the window's at the time the destination opens, not the one shown when a notification was
 * posted, so a notification still opens after a profile switch has replaced the home. Used on the window's thread.
 */
@Inject
@SingleIn(AppScope::class)
class HomeDestinations(
    private val openWindow: OpenWindow,
    private val profileManager: ProfileManager,
    private val migrationSessions: SourceMigrationSessionStore,
) {
    private var waiting: Waiting? = null

    // The profile the window's home last started in; null until it has started
    private var homeProfileId: Long? = null

    suspend fun open(destination: HomeDestination) {
        val profileId = when (val profile = profileOf(destination) ?: return) {
            DestinationProfile.Any -> {
                openInHome(destination, profileId = null)
                return
            }
            is DestinationProfile.Of -> profile.id
        }
        if (profileId == profileManager.activeProfileId) {
            openInHome(destination, profileId)
            return
        }
        when {
            profileManager.visibleProfiles.value.none { it.id == profileId } ->
                logcat(LogPriority.INFO) { "Not opening $destination: its profile isn't shown" }
            profileManager.profileRequiresUnlock(profileId) ->
                logcat(LogPriority.INFO) { "Not opening $destination: desktop can't unlock its profile" }
            else -> {
                waiting = Waiting(profileId, destination)
                profileManager.setActiveProfile(profileId)
            }
        }
    }

    /** Called each time the window's home starts a navigator: at launch and after each profile switch. */
    suspend fun homeStarted(navigator: Navigator) {
        homeProfileId = profileManager.activeProfileId
        val destination = waiting ?: return
        waiting = null
        if (destination.profileId == null || destination.profileId == homeProfileId) {
            navigate(navigator, destination.destination)
        }
    }

    /**
     * Opens [destination] in the home now if it shows the active profile, or else once it has started for it.
     * [profileId] is the profile it opens in, or null for whichever the home starts with.
     */
    private suspend fun openInHome(destination: HomeDestination, profileId: Long?) {
        val navigator = openWindow.context?.navigator
        if (navigator != null && homeProfileId == profileManager.activeProfileId) {
            navigate(navigator, destination)
        } else {
            waiting = Waiting(profileId, destination)
        }
    }

    /** The profile [destination] opens in, or null when it can't open. */
    private suspend fun profileOf(destination: HomeDestination): DestinationProfile? = when (destination) {
        is HomeDestination.Library -> DestinationProfile.Of(destination.profileId)
        is HomeDestination.Updates -> DestinationProfile.Of(destination.profileId)
        is HomeDestination.LibraryUpdateReport -> DestinationProfile.Of(destination.profileId)
        is HomeDestination.LibraryUpdateSettings -> DestinationProfile.Of(destination.profileId)
        is HomeDestination.StatisticsRecap -> DestinationProfile.Of(destination.profileId)
        is HomeDestination.SourceMigrationReview -> migrationSessions.get(destination.sessionId)
            ?.takeUnless { it.stage.isTerminal }
            ?.let { DestinationProfile.Of(it.profileId) }
        HomeDestination.Extensions, is HomeDestination.Link -> DestinationProfile.Any
    }

    private suspend fun navigate(navigator: Navigator, destination: HomeDestination) {
        // As on Android, the library opens over whatever shows and the others from the home's root
        if (destination !is HomeDestination.Library) navigator.popUntilRoot()
        when (destination) {
            is HomeDestination.Library -> HomeScreen.openTab(HomeScreen.Tab.Library())
            is HomeDestination.Updates -> HomeScreen.openTab(HomeScreen.Tab.Updates)
            is HomeDestination.LibraryUpdateReport -> navigator.push(LibraryUpdateReportScreen())
            is HomeDestination.LibraryUpdateSettings ->
                navigator.push(SettingsScreen(SettingsScreen.Destination.LibraryUpdates))
            is HomeDestination.StatisticsRecap -> navigator.push(StatisticsRecapScreen(destination.period))
            is HomeDestination.SourceMigrationReview ->
                navigator.push(SourceMigrationReviewScreen(destination.sessionId))
            HomeDestination.Extensions -> HomeScreen.openTab(HomeScreen.Tab.Browse(BrowseTab.Page.Extensions))
            is HomeDestination.Link -> navigator.openAppLink(destination.link)
        }
    }

    private sealed interface DestinationProfile {
        data class Of(val id: Long) : DestinationProfile

        /** Whichever profile the home shows. */
        data object Any : DestinationProfile
    }

    private class Waiting(val profileId: Long?, val destination: HomeDestination)
}
