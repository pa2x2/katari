package mihon.feature.profiles.ui

import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfilesPreferences
import mihon.feature.profiles.ui.switcher.ProfileSwitchEffects

/** Graph entry points of the profile picker, switcher and management screens, which have no view model to inject them. */
interface ProfilesGraph {
    val profileManager: ProfileManager
    val profilesPreferences: ProfilesPreferences
    val profileUnlock: ProfileUnlock
    val profileSwitchEffects: ProfileSwitchEffects
}
