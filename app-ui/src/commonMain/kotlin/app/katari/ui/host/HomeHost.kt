package app.katari.ui.host

import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import app.katari.sync.ui.notice.SyncStopMessages
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import eu.kanade.presentation.more.AppMigratingScreen
import eu.kanade.tachiyomi.ui.base.migration.rememberAppMigrating
import eu.kanade.tachiyomi.ui.home.HomeContentReadiness
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.home.LocalHomeContentReadiness
import eu.kanade.tachiyomi.ui.home.navigation.openSyncFromRoot
import eu.kanade.tachiyomi.ui.main.AppStateBannerState
import eu.kanade.tachiyomi.ui.main.HomeNavigatorContent
import eu.kanade.tachiyomi.ui.main.OnboardingOnFirstStart
import eu.kanade.tachiyomi.ui.main.rememberAppStateBannerState
import kotlinx.coroutines.CoroutineScope

/**
 * The home screen as a window of each platform hosts it. While app-upgrade or database migrations run, the app
 * migrating screen shows instead. Then [profileGate] shows `home` once a profile is open: the window's root navigator
 * around [HomeScreen], one per profile, with onboarding over it on the first start.
 *
 * Each time the navigator starts, [onNavigator] gets it, told whether this start is the app's launch, which
 * [launchState] keeps track of. On the launch, [AppLaunch] runs next, with [launchScope] for its background work.
 * [onReady] is called after that, while the migrating screen shows, and once the home's first tab has content.
 *
 * @param profileGate shows `home` once a profile is open, and the platform's profile picker or unlock before; it gets
 * the banners the home shows, by which Android colours its status bar.
 * @param overlay drawn over the screens, such as a scrim behind a see-through system navigation bar.
 * @param navigatorContent the platform's additions for the navigator, such as prompts over its screens.
 */
@HostEntryPoint
@Composable
fun HomeHost(
    launchState: HomeLaunchState,
    launchScope: CoroutineScope,
    onReady: () -> Unit,
    profileGate: @Composable (banners: AppStateBannerState, home: @Composable () -> Unit) -> Unit,
    onNavigator: suspend (navigator: Navigator, isLaunch: Boolean) -> Unit = { _, _ -> },
    overlay: @Composable BoxScope.() -> Unit = {},
    navigatorContent: @Composable (Navigator) -> Unit = {},
    offersOnboarding: Boolean = true,
) {
    val currentOnReady by rememberUpdatedState(onReady)
    val currentOnNavigator by rememberUpdatedState(onNavigator)
    if (rememberAppMigrating()) {
        AppMigratingScreen()
        // Android lifts its splash screen, so the reason for the wait shows
        LaunchedEffect(Unit) { currentOnReady() }
        return
    }

    val graph = LocalAppUiGraph.current
    val banners = rememberAppStateBannerState()
    val readiness = remember { HomeContentReadiness { currentOnReady() } }
    profileGate(banners) {
        val activeProfile by graph.profileManager.activeProfile.collectAsState()
        val activeProfileId = activeProfile?.id ?: graph.profileManager.activeProfileId
        CompositionLocalProvider(LocalHomeContentReadiness provides readiness) {
            Navigator(
                screen = HomeScreen,
                key = "MainNavigator:$activeProfileId",
                disposeBehavior = NavigatorDisposeBehavior(
                    disposeNestedNavigators = false,
                    disposeSteps = true,
                ),
            ) { navigator ->
                // Voyager keeps the navigator across a key change, so a profile switch restarts this to carry out the
                // intent that asked for the switch.
                LaunchedEffect(navigator, activeProfileId) {
                    val isLaunch = !launchState.isCompleted
                    launchState.isCompleted = true
                    currentOnNavigator(navigator, isLaunch)
                    if (isLaunch) graph.appLaunch.run(launchScope)
                    currentOnReady()
                }
                HomeNavigatorContent(navigator = navigator, banners = banners, overlay = overlay)
                SyncStopMessages(onOpen = { openSyncFromRoot(navigator) })
                navigatorContent(navigator)
                if (offersOnboarding) OnboardingOnFirstStart()
            }
        }
    }
}
