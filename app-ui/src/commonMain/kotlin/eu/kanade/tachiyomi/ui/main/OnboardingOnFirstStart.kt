package eu.kanade.tachiyomi.ui.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import app.katari.ui.di.LocalAppUiGraph
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.tachiyomi.ui.more.OnboardingScreen

/** Opens onboarding over the home screen until the user has been through it once. */
@Composable
fun OnboardingOnFirstStart() {
    val navigator = LocalNavigator.currentOrThrow
    val onboardingPreferences = LocalAppUiGraph.current.onboardingPreferences

    LaunchedEffect(Unit) {
        // After process death the stack comes back with onboarding under the screens it opened, such as Bring over
        if (!onboardingPreferences.shownOnboardingFlow.get() && navigator.items.none { it is OnboardingScreen }) {
            navigator.push(OnboardingScreen())
        }
    }
}
