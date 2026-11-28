package eu.kanade.tachiyomi.ui.more

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import app.katari.presentation.kit.navigation.FullWindowContent
import app.katari.sync.ui.signin.SyncJoinOrigin
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.onboarding.OnboardingScreen
import eu.kanade.presentation.util.Screen
import tachiyomi.presentation.core.util.collectAsState

class OnboardingScreen : Screen(), SyncJoinOrigin {

    @Composable
    override fun Content() {
        val graph = LocalAppUiGraph.current
        val navigator = LocalNavigator.currentOrThrow

        val onboardingPreferences = remember(graph) { graph.onboardingPreferences }
        val shownOnboardingFlow by onboardingPreferences.shownOnboardingFlow.collectAsState()

        val finishOnboarding: () -> Unit = {
            onboardingPreferences.shownOnboardingFlow.set(true)
            navigator.pop()
        }

        // Prevent exiting if onboarding hasn't been completed
        NavigationBackHandler(
            state = rememberNavigationEventState(NavigationEventInfo.None),
            isBackEnabled = !shownOnboardingFlow,
            onBackCompleted = {},
        )

        FullWindowContent(Modifier.fillMaxSize()) {
            OnboardingScreen(onComplete = finishOnboarding)
        }
    }
}
