package mihon.desktop.onboarding

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.more.OnboardingDestination

/** Desktop can't show the first-run guide yet, so the advanced settings leave it out. Stands in until it can. */
@Inject
class NoOnboardingDestination : OnboardingDestination {
    override val isAvailable: Boolean = false

    override fun screen(): Screen = error("The first-run guide is not available on desktop")
}
