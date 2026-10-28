package mihon.desktop.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import cafe.adriel.voyager.navigator.Navigator
import cafe.adriel.voyager.navigator.NavigatorDisposeBehavior
import eu.kanade.tachiyomi.ui.home.HomeContentReadiness
import eu.kanade.tachiyomi.ui.home.HomeScreen
import eu.kanade.tachiyomi.ui.home.LocalHomeContentReadiness
import eu.kanade.tachiyomi.ui.main.HomeNavigatorContent
import eu.kanade.tachiyomi.ui.main.OnboardingOnFirstStart
import eu.kanade.tachiyomi.ui.main.rememberAppStateBannerState
import mihon.app.ui.di.LocalAppUiGraph
import mihon.desktop.profile.StartupProfileGate
import mihon.feature.profiles.core.ProfileStartupSnapshot

/**
 * The window's content: the startup profile gate, then the home and the screens over it, with onboarding over them on
 * the first start, as Android's main activity shows them. Desktop has no app-upgrade migrations to wait for, and no
 * shortcuts, notifications or links that open a screen.
 */
@Composable
fun HomeWindowContent(startup: ProfileStartupSnapshot) {
    val graph = LocalAppUiGraph.current
    val banners = rememberAppStateBannerState()
    StartupProfileGate(startup) {
        LaunchedEffect(Unit) { graph.launchCleanup.run(this) }

        val activeProfile by graph.profileManager.activeProfile.collectAsState()
        val activeProfileId = activeProfile?.id ?: graph.profileManager.activeProfileId
        CompositionLocalProvider(LocalHomeContentReadiness provides WindowShownReadiness) {
            Navigator(
                screen = HomeScreen,
                key = "MainNavigator:$activeProfileId",
                disposeBehavior = NavigatorDisposeBehavior(
                    disposeNestedNavigators = false,
                    disposeSteps = true,
                ),
            ) { navigator ->
                HomeNavigatorContent(navigator = navigator, banners = banners)
                OnboardingOnFirstStart()
            }
        }
    }
}

// The window shows from its first frame, with no splash screen to lift once the home has content
private val WindowShownReadiness = HomeContentReadiness {}
