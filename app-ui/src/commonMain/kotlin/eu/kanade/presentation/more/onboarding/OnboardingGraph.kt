package eu.kanade.presentation.more.onboarding

import eu.kanade.domain.base.OnboardingPreferences
import eu.kanade.tachiyomi.core.security.PrivacyPreferences

/** Graph entry points of the first-run guide, beyond the appearance and storage settings its steps share. */
interface OnboardingGraph {
    val onboardingPreferences: OnboardingPreferences
    val privacyPreferences: PrivacyPreferences
    val onboardingPermissions: OnboardingPermissions
    val mihonApp: MihonApp
}
