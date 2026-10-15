package eu.kanade.presentation.more.onboarding

import eu.kanade.domain.ui.UiPreferences
import eu.kanade.tachiyomi.core.security.PrivacyPreferences
import tachiyomi.domain.storage.service.StoragePreferences

/** Graph entry points of the onboarding steps. */
interface OnboardingPresentationGraph {
    val privacyPreferences: PrivacyPreferences
    val storagePreferences: StoragePreferences
    val uiPreferences: UiPreferences
}
