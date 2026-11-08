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
        if (!onboardingPreferences.shownOnboardingFlow.get() && navigator.lastItem !is OnboardingScreen) {
            navigator.push(OnboardingScreen())
        }
    }
}
