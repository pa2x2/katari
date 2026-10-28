package eu.kanade.tachiyomi.ui.more

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen

@Inject
class OnboardingScreenDestination : OnboardingDestination {
    override val isAvailable: Boolean = true

    override fun screen(): Screen = OnboardingScreen()
}
