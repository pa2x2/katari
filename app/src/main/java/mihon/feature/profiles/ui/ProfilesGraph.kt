package mihon.feature.profiles.ui

import eu.kanade.domain.ui.UiPreferences
import mihon.feature.profiles.core.ProfileManager

/** Graph entry points of the profile picker and management screens, which have no view model to inject them. */
interface ProfilesGraph {
    val profileManager: ProfileManager
    val uiPreferences: UiPreferences
}
