package eu.kanade.tachiyomi.ui.home.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import app.katari.core.common.CustomPreferences
import app.katari.core.common.HomeScreenTabs
import app.katari.core.common.homeScreenContentTabOrder
import app.katari.core.common.navigation.HomeNavigationConfiguration
import app.katari.core.common.navigation.resolveHomeNavigationConfiguration
import app.katari.core.common.navigation.withVisibleProfiles
import app.katari.core.common.resolveHomeScreenTab
import app.katari.core.common.resolveVisibleHomeScreenTabs
import app.katari.core.common.toHomeScreenTabs
import app.katari.ui.di.LocalAppUiGraph
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.runtime.collectAsState as collectFlowAsState

/**
 * The active profile's home destinations as this platform offers them, and the tab the home opens on. Read beside the
 * root navigator rather than in the home, so the dock shown beside a screen pushed over the home has them before the
 * home has first shown, and follows changes made to them while the home is covered.
 */
internal class HomeTabSettings(
    val profileId: Long,
    val preferences: CustomPreferences,
    val startupTab: HomeScreenTabs,
    val configuredTabs: Set<HomeScreenTabs>,
    val tabOrder: List<HomeScreenTabs>,
    primaryTabs: List<HomeScreenTabs>,
    val showProfilesTab: Boolean,
) {
    val configuration: HomeNavigationConfiguration = resolveHomeNavigationConfiguration(
        enabledTabs = configuredTabs,
        tabOrder = tabOrder,
        primaryTabs = primaryTabs,
    )
        .withVisibleProfiles(showProfilesTab)

    val contentTabs: List<HomeScreenTabs> = configuration.enabledTabs.filter { it in homeScreenContentTabOrder }

    val launchTab: HomeScreenTabs = resolveHomeLaunchTab(startupTab, configuredTabs, tabOrder, showProfilesTab)

    /** The tab the home opens on: the one it was last on in this process, else [launchTab]. */
    fun openingTab(): HomeScreenTabs =
        lastHomeTabByProfile[profileId]?.let { resolveHomeScreenTab(it, contentTabs, tabOrder) } ?: launchTab
}

/**
 * The tab each profile's home was last on, so switching back returns to it. Kept for the process only: launching the
 * app opens the profile's startup tab.
 */
internal val lastHomeTabByProfile = mutableMapOf<Long, HomeScreenTabs>()

/** [configuredTab] if the home shows it, else the content tab that takes its place. */
internal fun resolveHomeLaunchTab(
    configuredTab: HomeScreenTabs,
    configuredTabs: Set<HomeScreenTabs>,
    tabOrder: Collection<HomeScreenTabs>,
    showProfilesTab: Boolean,
): HomeScreenTabs {
    val enabledTabs = resolveVisibleHomeScreenTabs(
        tabs = configuredTabs,
        tabOrder = tabOrder,
        showProfilesTab = showProfilesTab,
    )
    val contentTabs = enabledTabs.filter { it in homeScreenContentTabOrder }
    return resolveHomeScreenTab(configuredTab, contentTabs, tabOrder)
}

@Composable
internal fun rememberHomeTabSettings(): HomeTabSettings {
    val graph = LocalAppUiGraph.current
    val profileManager = graph.profileManager
    val profileStore = graph.profileStore
    val activeProfile by profileManager.activeProfile.collectFlowAsState()
    val profileId = activeProfile?.id ?: profileStore.currentProfileId
    val preferences = remember(profileStore, profileId) { CustomPreferences(profileStore.profileStore(profileId)) }
    val startupTab by preferences.homeScreenStartupTab.collectAsState()
    val tabsPreference by preferences.homeScreenTabs.collectAsState()
    val tabOrder by preferences.homeScreenTabOrder.collectAsState()
    val primaryTabs by preferences.homeScreenPrimaryTabs.collectAsState()
    val visibleProfiles by profileManager.visibleProfiles.collectFlowAsState()
    val unofferedTabs = remember(graph) { graph.unofferedHomeTabs() }
    val showProfilesTab = visibleProfiles.size > 1
    return remember(preferences, startupTab, tabsPreference, tabOrder, primaryTabs, showProfilesTab, unofferedTabs) {
        HomeTabSettings(
            profileId = profileId,
            preferences = preferences,
            startupTab = startupTab,
            configuredTabs = tabsPreference.toHomeScreenTabs() - unofferedTabs,
            tabOrder = tabOrder,
            primaryTabs = primaryTabs,
            showProfilesTab = showProfilesTab,
        )
    }
}
