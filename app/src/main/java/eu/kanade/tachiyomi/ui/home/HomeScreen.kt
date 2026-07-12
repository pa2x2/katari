package eu.kanade.tachiyomi.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.util.fastForEach
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.TabNavigator
import eu.kanade.domain.ui.UiPreferences
import eu.kanade.presentation.util.Screen
import eu.kanade.presentation.util.isTabletUi
import eu.kanade.tachiyomi.extension.ExtensionManager
import eu.kanade.tachiyomi.ui.browse.BrowseTab
import eu.kanade.tachiyomi.ui.download.DownloadQueueScreen
import eu.kanade.tachiyomi.ui.entry.EntryScreen
import eu.kanade.tachiyomi.ui.history.HistoryTab
import eu.kanade.tachiyomi.ui.library.LibraryTab
import eu.kanade.tachiyomi.ui.more.MoreTab
import eu.kanade.tachiyomi.ui.updates.UpdatesTab
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import mihon.core.common.CustomPreferences
import mihon.core.common.HomeScreenTabs
import mihon.core.common.homeScreenContentTabOrder
import mihon.core.common.resolveHomeScreenTab
import mihon.core.common.resolveVisibleHomeScreenTabs
import mihon.core.common.toHomeScreenTabs
import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfileStore
import mihon.feature.profiles.ui.ProfilePickerScreen
import mihon.feature.profiles.ui.handleProfileShortcut
import soup.compose.material.motion.animation.materialFadeThroughIn
import soup.compose.material.motion.animation.materialFadeThroughOut
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.NavigationBar
import tachiyomi.presentation.core.components.material.NavigationRail
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import androidx.compose.runtime.collectAsState as collectFlowAsState
import cafe.adriel.voyager.navigator.tab.Tab as VoyagerTab

object HomeScreen : Screen() {

    private val librarySearchEvent = Channel<String>()
    private val openTabEvent = Channel<Tab>()
    private val showBottomNavEvent = Channel<Boolean>()

    @Suppress("ConstPropertyName")
    private const val TabFadeDuration = 200

    @Suppress("ConstPropertyName")
    private const val TabNavigatorKey = "HomeTabs"

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val profileManager = remember { Injekt.get<ProfileManager>() }
        val profileStore = remember { Injekt.get<ProfileStore>() }
        val uiPreferences = remember { Injekt.get<UiPreferences>() }
        val activeProfile by profileManager.activeProfile.collectFlowAsState()
        val activeProfileId = activeProfile?.id ?: profileStore.currentProfileId
        val customPreferences = remember(activeProfileId) {
            CustomPreferences(profileStore.profileStore(activeProfileId))
        }
        val configuredTab by customPreferences.homeScreenStartupTab.collectAsState()
        val configuredTabs by customPreferences.homeScreenTabs.collectAsState()
        val configuredTabOrder by customPreferences.homeScreenTabOrder.collectAsState()
        val visibleProfiles by profileManager.visibleProfiles.collectFlowAsState()
        val enabledTabs = remember(configuredTabs, configuredTabOrder, visibleProfiles) {
            resolveVisibleHomeScreenTabs(
                tabs = configuredTabs.toHomeScreenTabs(),
                tabOrder = configuredTabOrder,
                showProfilesTab = visibleProfiles.size > 1,
            )
        }
        val contentTabs = remember(enabledTabs) {
            enabledTabs.filter { it in homeScreenContentTabOrder }
        }
        val launchTab = remember(
            configuredTab,
            configuredTabs,
            configuredTabOrder,
            visibleProfiles.size,
        ) {
            resolveLaunchTab(
                configuredTab = configuredTab,
                configuredTabs = configuredTabs,
                configuredTabOrder = configuredTabOrder,
                showProfilesTab = visibleProfiles.size > 1,
            )
        }
        val renderedTabs = remember(enabledTabs) {
            enabledTabs
        }
        val fallbackTab = remember(contentTabs, configuredTabOrder) {
            resolveContentTab(resolveHomeScreenTab(HomeScreenTabs.Library, contentTabs, configuredTabOrder))
        }
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        TabNavigator(
            tab = launchTab,
            key = "$TabNavigatorKey:$activeProfileId",
        ) { tabNavigator ->
            // Provide usable navigator to content screen
            CompositionLocalProvider(LocalNavigator provides navigator) {
                Scaffold(
                    startBar = {
                        if (isTabletUi()) {
                            NavigationRail {
                                renderedTabs.fastForEach {
                                    if (it == HomeScreenTabs.Profiles) {
                                        ProfileShortcutNavigationRailItem(onClick = {
                                            scope.launch {
                                                handleProfileShortcut(
                                                    context = context,
                                                    profileManager = profileManager,
                                                    uiPreferences = uiPreferences,
                                                    onOpenProfilePicker = { navigator.push(ProfilePickerScreen()) },
                                                    onBeforeSwitch = { navigator.popUntilRoot() },
                                                )
                                            }
                                        })
                                    } else {
                                        TabNavigationRailItem(resolveContentTab(it))
                                    }
                                }
                            }
                        }
                    },
                    bottomBar = {
                        if (!isTabletUi()) {
                            val bottomNavVisible by produceState(initialValue = true) {
                                showBottomNavEvent.receiveAsFlow().collectLatest { value = it }
                            }
                            AnimatedVisibility(
                                visible = bottomNavVisible,
                                enter = expandVertically(),
                                exit = shrinkVertically(),
                            ) {
                                NavigationBar {
                                    renderedTabs.fastForEach {
                                        if (it == HomeScreenTabs.Profiles) {
                                            ProfileShortcutNavigationBarItem(
                                                onClick = {
                                                    scope.launch {
                                                        handleProfileShortcut(
                                                            context = context,
                                                            profileManager = profileManager,
                                                            uiPreferences = uiPreferences,
                                                            onOpenProfilePicker = {
                                                                navigator.push(ProfilePickerScreen())
                                                            },
                                                            onBeforeSwitch = { navigator.popUntilRoot() },
                                                        )
                                                    }
                                                },
                                            )
                                        } else {
                                            TabNavigationBarItem(resolveContentTab(it))
                                        }
                                    }
                                }
                            }
                        }
                    },
                    contentWindowInsets = WindowInsets(0),
                ) { contentPadding ->
                    Box(
                        modifier = Modifier
                            .padding(contentPadding)
                            .consumeWindowInsets(contentPadding),
                    ) {
                        AnimatedContent(
                            targetState = tabNavigator.current,
                            transitionSpec = {
                                materialFadeThroughIn(initialScale = 1f, durationMillis = TabFadeDuration) togetherWith
                                    materialFadeThroughOut(durationMillis = TabFadeDuration)
                            },
                            label = "tabContent",
                        ) {
                            tabNavigator.saveableState(key = "currentTab", it) {
                                it.Content()
                            }
                        }
                    }
                }
            }

            val goToFallbackTab = { tabNavigator.current = fallbackTab }

            BackHandler(enabled = tabNavigator.current != fallbackTab, onBack = goToFallbackTab)

            var previousProfileId by rememberSaveable { mutableStateOf<Long?>(null) }

            LaunchedEffect(activeProfileId) {
                val currentProfileId = activeProfileId
                val lastProfileId = previousProfileId
                previousProfileId = currentProfileId

                if (lastProfileId == null || currentProfileId == lastProfileId) {
                    return@LaunchedEffect
                }

                val profilePreferences = CustomPreferences(profileStore.profileStore(currentProfileId))
                val profileLaunchTab = resolveLaunchTab(
                    configuredTab = profilePreferences.homeScreenStartupTab.get(),
                    configuredTabs = profilePreferences.homeScreenTabs.get(),
                    configuredTabOrder = profilePreferences.homeScreenTabOrder.get(),
                    showProfilesTab = visibleProfiles.size > 1,
                )

                if (tabNavigator.current::class != profileLaunchTab::class) {
                    tabNavigator.current = profileLaunchTab
                }
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
                    customPreferences.homeScreenStartupTab.set(resolvedStartupTab)
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
                            if (HomeScreenTabs.Profiles in enabledTabs) {
                                handleProfileShortcut(
                                    context = context,
                                    profileManager = profileManager,
                                    uiPreferences = uiPreferences,
                                    onOpenProfilePicker = { navigator.push(ProfilePickerScreen()) },
                                    onBeforeSwitch = { navigator.popUntilRoot() },
                                )
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
                            Tab.Profiles -> error("Handled above")
                        }
                        val resolvedTab =
                            resolveContentTab(resolveHomeScreenTab(requestedTab, contentTabs, configuredTabOrder))
                        tabNavigator.current = resolvedTab

                        if (it is Tab.Browse && resolvedTab::class == BrowseTab::class && it.toExtensions) {
                            BrowseTab.showExtension()
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

    @Composable
    private fun RowScope.TabNavigationBarItem(tab: eu.kanade.presentation.util.Tab) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationBarItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = true,
        )
    }

    @Composable
    private fun TabNavigationRailItem(tab: eu.kanade.presentation.util.Tab) {
        val tabNavigator = LocalTabNavigator.current
        val navigator = LocalNavigator.currentOrThrow
        val scope = rememberCoroutineScope()
        val selected = tabNavigator.current::class == tab::class
        NavigationRailItem(
            selected = selected,
            onClick = {
                if (!selected) {
                    tabNavigator.current = tab
                } else {
                    scope.launch { tab.onReselect(navigator) }
                }
            },
            icon = { NavigationIconItem(tab) },
            label = {
                Text(
                    text = tab.options.title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = true,
        )
    }

    @Composable
    private fun RowScope.ProfileShortcutNavigationBarItem(onClick: () -> Unit) {
        val title = stringResource(MR.strings.action_switch)
        val contentDescription = stringResource(MR.strings.profiles_switch_summary)
        NavigationBarItem(
            selected = false,
            onClick = onClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = contentDescription,
                )
            },
            label = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = true,
        )
    }

    @Composable
    private fun ProfileShortcutNavigationRailItem(onClick: () -> Unit) {
        val title = stringResource(MR.strings.action_switch)
        val contentDescription = stringResource(MR.strings.profiles_switch_summary)
        NavigationRailItem(
            selected = false,
            onClick = onClick,
            icon = {
                Icon(
                    imageVector = Icons.Outlined.AccountCircle,
                    contentDescription = contentDescription,
                )
            },
            label = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            alwaysShowLabel = true,
        )
    }

    @Composable
    private fun NavigationIconItem(tab: eu.kanade.presentation.util.Tab) {
        BadgedBox(
            badge = {
                when {
                    tab is UpdatesTab -> {
                        val count by produceState(initialValue = 0) {
                            val pref = Injekt.get<LibraryPreferences>()
                            combine(
                                pref.newShowUpdatesCount.changes(),
                                pref.newUpdatesCount.changes(),
                            ) { show, count -> if (show) count else 0 }
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.notification_updates_generic,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                    tab is BrowseTab -> {
                        val count by produceState(initialValue = 0) {
                            val extensionManager = Injekt.get<ExtensionManager>()
                            combine(
                                extensionManager.pendingUpdatesCount,
                                extensionManager.isAutoUpdateInProgress,
                            ) { count, inProgress ->
                                if (inProgress) 0 else count
                            }
                                .collectLatest { value = it }
                        }
                        if (count > 0) {
                            Badge {
                                val desc = pluralStringResource(
                                    MR.plurals.update_check_notification_ext_updates,
                                    count = count,
                                    count,
                                )
                                Text(
                                    text = count.toString(),
                                    modifier = Modifier.semantics { contentDescription = desc },
                                )
                            }
                        }
                    }
                }
            },
        ) {
            Icon(
                painter = tab.options.icon!!,
                contentDescription = tab.options.title,
            )
        }
    }

    suspend fun search(query: String) {
        librarySearchEvent.send(query)
    }

    suspend fun openTab(tab: Tab) {
        openTabEvent.send(tab)
    }

    suspend fun showBottomNav(show: Boolean) {
        showBottomNavEvent.send(show)
    }

    private fun resolveVisibleTab(
        tab: VoyagerTab,
        contentTabs: Collection<HomeScreenTabs>,
        tabOrder: Collection<HomeScreenTabs>,
    ): eu.kanade.presentation.util.Tab {
        return resolveContentTab(resolveHomeScreenTab(tab.toHomeScreenTab(), contentTabs, tabOrder))
    }

    private fun resolveLaunchTab(
        configuredTab: HomeScreenTabs,
        configuredTabs: Set<String>,
        configuredTabOrder: Collection<HomeScreenTabs>,
        showProfilesTab: Boolean,
    ): eu.kanade.presentation.util.Tab {
        val enabledTabs = resolveVisibleHomeScreenTabs(
            tabs = configuredTabs.toHomeScreenTabs(),
            tabOrder = configuredTabOrder,
            showProfilesTab = showProfilesTab,
        )
        val contentTabs = enabledTabs.filter { it in homeScreenContentTabOrder }
        return resolveContentTab(resolveHomeScreenTab(configuredTab, contentTabs, configuredTabOrder))
    }

    private fun resolveContentTab(tab: HomeScreenTabs): eu.kanade.presentation.util.Tab {
        return when (tab) {
            HomeScreenTabs.Library -> LibraryTab
            HomeScreenTabs.Updates -> UpdatesTab
            HomeScreenTabs.History -> HistoryTab
            HomeScreenTabs.Browse -> BrowseTab
            HomeScreenTabs.More -> MoreTab
            HomeScreenTabs.Profiles -> error("Profiles is a navigation item, not a content tab")
        }
    }

    private fun VoyagerTab.toHomeScreenTab(): HomeScreenTabs {
        return when (this) {
            is LibraryTab -> HomeScreenTabs.Library
            is UpdatesTab -> HomeScreenTabs.Updates
            is HistoryTab -> HomeScreenTabs.History
            is BrowseTab -> HomeScreenTabs.Browse
            is MoreTab -> HomeScreenTabs.More
            else -> HomeScreenTabs.More
        }
    }

    sealed interface Tab {
        data class Library(
            val entryIdToOpen: Long? = null,
        ) : Tab
        data object Updates : Tab
        data object History : Tab
        data class Browse(val toExtensions: Boolean = false) : Tab
        data class More(val toDownloads: Boolean) : Tab
        data object Profiles : Tab
    }
}
