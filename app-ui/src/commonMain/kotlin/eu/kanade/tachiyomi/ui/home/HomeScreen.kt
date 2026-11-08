package eu.kanade.tachiyomi.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.core.common.CustomPreferences
import app.katari.core.common.HomeScreenTabs
import app.katari.core.common.resolveHomeScreenTab
import app.katari.core.common.toHomeScreenTabs
import app.katari.presentation.kit.navigation.FloatingDockHost
import app.katari.presentation.kit.window.currentWindowLayout
import app.katari.profiles.ui.switcher.LocalProfileSwitcher
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.entry.search.EntrySearchOrigin
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.home.navigation.HomeNavigationBar
import eu.kanade.tachiyomi.ui.home.navigation.LocalHomeNavigation
import eu.kanade.tachiyomi.ui.home.navigation.lastHomeTabByProfile
import eu.kanade.tachiyomi.ui.home.navigation.resolveHomeLaunchTab
import eu.kanade.tachiyomi.ui.home.navigation.unofferedHomeTabs
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.stats.StatsScreen
import eu.kanade.tachiyomi.ui.translator.TranslatorTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import cafe.adriel.voyager.navigator.tab.Tab as VoyagerTab

object HomeScreen : Screen(), EntrySearchOrigin {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()

    @Suppress("ConstPropertyName")
    private const val TabFadeDuration = 200

    @Suppress("ConstPropertyName")
    private const val TabNavigatorKey = "HomeTabs"

    @Composable
    override fun Content() {
        val graph = LocalAppUiGraph.current
        val profileStore = remember { graph.profileStore }
        val homeNavigation = LocalHomeNavigation.current
        val tabSettings = homeNavigation.tabSettings
        val activeProfileId = tabSettings.profileId
        val configuredTab = tabSettings.startupTab
        val configuredTabOrder = tabSettings.tabOrder
        val navigationConfiguration = tabSettings.configuration
        val enabledTabs = navigationConfiguration.enabledTabs
        val contentTabs = tabSettings.contentTabs
        val fallbackTab = remember(contentTabs, configuredTabOrder) {
            resolveContentTab(resolveHomeScreenTab(HomeScreenTabs.Library, contentTabs, configuredTabOrder))
        }
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val profileSwitcher by rememberUpdatedState(LocalProfileSwitcher.current)
        TabNavigator(
            tab = resolveContentTab(tabSettings.openingTab()),
            key = "$TabNavigatorKey:$activeProfileId",
        ) { tabNavigator ->
            val selectedTab = tabNavigator.current.toHomeScreenTab()
            val onNavigationClick: (HomeScreenTabs) -> Unit = { tab ->
                when (tab) {
                    HomeScreenTabs.Profiles -> profileSwitcher?.open()

                    HomeScreenTabs.Statistics -> navigator.push(StatsScreen())
                    else -> {
                        val destination = resolveContentTab(tab)
                        if (tabNavigator.current::class != destination::class) {
                            tabNavigator.current = destination
                        } else {
                            scope.launch { destination.onReselect(navigator) }
                        }
                    }
                }
            }
            SideEffect {
                homeNavigation.selectedTab = selectedTab
            }
            val currentOnNavigationClick by rememberUpdatedState(onNavigationClick)
            LaunchedEffect(homeNavigation) {
                snapshotFlow { homeNavigation.requestedTab }
                    .filterNotNull()
                    .collect {
                        homeNavigation.requestedTab = null
                        currentOnNavigationClick(it)
                    }
            }
            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                // A wide window shows these beside every screen instead, through HomeSideNavigationHost
                FloatingDockHost(
                    dockVisible = !currentWindowLayout().usesSideNavigation && !homeNavigation.hiddenByTab,
                    dock = {
                        HomeNavigationBar(
                            primaryTabs = navigationConfiguration.primaryTabs,
                            overflowTabs = navigationConfiguration.overflowTabs,
                            selectedTab = selectedTab,
                            onClick = onNavigationClick,
                        )
                    },
                ) {
                    AnimatedContent(
                        targetState = tabNavigator.current,
                        transitionSpec = {
                            materialFadeThroughIn(initialScale = 1f, durationMillis = TabFadeDuration)
                                .togetherWith(materialFadeThroughOut(durationMillis = TabFadeDuration))
                        },
                        label = "tabContent",
                    ) {
                        tabNavigator.saveableState(key = "currentTab", it) {
                            it.Content()
                        }
                    }
                }
            }

            val goToFallbackTab = { tabNavigator.current = fallbackTab }

            NavigationBackHandler(
                state = rememberNavigationEventState(NavigationEventInfo.None),
                isBackEnabled = tabNavigator.current != fallbackTab,
                onBackCompleted = goToFallbackTab,
            )

            var previousProfileId by rememberSaveable { mutableStateOf<Long?>(null) }

            LaunchedEffect(activeProfileId) {
                val currentProfileId = activeProfileId
                val lastProfileId = previousProfileId
                previousProfileId = currentProfileId

                if (lastProfileId == null || currentProfileId == lastProfileId) {
                    return@LaunchedEffect
                }

                val profilePreferences = CustomPreferences(profileStore.profileStore(currentProfileId))
                val profileLaunchTab = resolveContentTab(
                    resolveHomeLaunchTab(
                        configuredTab = lastHomeTabByProfile[currentProfileId]
                            ?: profilePreferences.homeScreenStartupTab.get(),
                        configuredTabs = profilePreferences.homeScreenTabs.get().toHomeScreenTabs() -
                            graph.unofferedHomeTabs(),
                        tabOrder = profilePreferences.homeScreenTabOrder.get(),
                        showProfilesTab = tabSettings.showProfilesTab,
                    ),
                )

                if (tabNavigator.current::class != profileLaunchTab::class) {
                    tabNavigator.current = profileLaunchTab
                }
            }

            // Reads the tab when it runs rather than when composed, so a switch's tab change above isn't recorded
            // under the profile that was just left.
            LaunchedEffect(activeProfileId, tabNavigator.current) {
                lastHomeTabByProfile[activeProfileId] = tabNavigator.current.toHomeScreenTab()
            }

            LaunchedEffect(contentTabs, configuredTabOrder) {
                val resolvedCurrentTab = resolveVisibleTab(
                    tabNavigator.current,
                    contentTabs,
                    configuredTabOrder,
                )
                if (resolvedCurrentTab::class != tabNavigator.current::class) {
                    tabNavigator.current = resolvedCurrentTab
                }
                val resolvedStartupTab = resolveHomeScreenTab(configuredTab, contentTabs, configuredTabOrder)
                if (resolvedStartupTab != configuredTab) {
                    tabSettings.preferences.homeScreenStartupTab.set(resolvedStartupTab)
                }
            }

            LaunchedEffect(contentTabs, enabledTabs, configuredTabOrder, fallbackTab) {
                launch {
                    librarySearchEvent.receiveAsFlow().collectLatest {
                        if (HomeScreenTabs.Library in contentTabs) {
                            tabNavigator.current = resolveContentTab(HomeScreenTabs.Library)
                            LibraryTab.search(it)
                        } else {
                            goToFallbackTab()
                        }
                    }
                }
                launch {
                    openTabEvent.receiveAsFlow().collectLatest {
                        if (it == Tab.Profiles) {
                            val switcher = profileSwitcher
                            if (switcher != null) {
                                switcher.open()
                            } else {
                                goToFallbackTab()
                            }
                            return@collectLatest
                        }
                        val requestedTab = when (it) {
                            is Tab.Library -> HomeScreenTabs.Library
                            Tab.Updates -> HomeScreenTabs.Updates
                            Tab.History -> HomeScreenTabs.History
                            is Tab.Browse -> HomeScreenTabs.Browse
                            is Tab.More -> HomeScreenTabs.More
                            Tab.Translator -> HomeScreenTabs.Translator
                            Tab.Profiles -> error("Handled above")
                        }
                        val resolvedTab =
                            resolveContentTab(resolveHomeScreenTab(requestedTab, contentTabs, configuredTabOrder))
                        tabNavigator.current = resolvedTab

                        if (it is Tab.Browse && resolvedTab::class == BrowseTab::class && it.page != null) {
                            BrowseTab.showPage(it.page)
                        }

                        if (it is Tab.Library && it.entryIdToOpen != null && resolvedTab::class == LibraryTab::class) {
                            navigator.push(EntryScreen(it.entryIdToOpen))
                        }
                        if (it is Tab.More && resolvedTab::class == MoreTab::class && it.toDownloads) {
                            navigator.push(DownloadQueueScreen)
                        }
                    }
                }
            }
        }
    }

    override suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    private fun resolveVisibleTab(
        tab: VoyagerTab,
        contentTabs: Collection<HomeScreenTabs>,
        tabOrder: Collection<HomeScreenTabs>,
    ): eu.kanade.presentation.util.Tab {
        return resolveContentTab(resolveHomeScreenTab(tab.toHomeScreenTab(), contentTabs, tabOrder))
    }

    private fun resolveContentTab(tab: HomeScreenTabs): eu.kanade.presentation.util.Tab {
        return when (tab) {
            HomeScreenTabs.Library -> LibraryTab
            HomeScreenTabs.Updates -> UpdatesTab
            HomeScreenTabs.History -> HistoryTab
            HomeScreenTabs.Browse -> BrowseTab
            HomeScreenTabs.More -> MoreTab
            HomeScreenTabs.Profiles -> error("Profiles is a navigation item, not a content tab")
            HomeScreenTabs.Translator -> TranslatorTab
            HomeScreenTabs.Statistics -> error("Statistics is a navigation shortcut, not a content tab")
        }
    }

    private fun VoyagerTab.toHomeScreenTab(): HomeScreenTabs {
        return when (this) {
            is LibraryTab -> HomeScreenTabs.Library
            is UpdatesTab -> HomeScreenTabs.Updates
            is HistoryTab -> HomeScreenTabs.History
            is BrowseTab -> HomeScreenTabs.Browse
            is MoreTab -> HomeScreenTabs.More
            is TranslatorTab -> HomeScreenTabs.Translator
            else -> HomeScreenTabs.More
        }
    }

    sealed interface Tab {
        data class Library(
            val entryIdToOpen: Long? = null,
        ) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val page: BrowseTab.Page? = null) : Tab
        data class More(val toDownloads: Boolean) : Tab
        data object Translator : Tab
        data object Profiles : Tab
    }
}
