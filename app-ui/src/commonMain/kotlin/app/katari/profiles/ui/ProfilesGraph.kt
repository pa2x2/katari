package app.katari.profiles.ui

import app.katari.entry.interactions.lifecycle.removal.EntryDestructiveRemovalFeature
import app.katari.profiles.core.ProfileManager
import app.katari.profiles.core.ProfilesPreferences
import app.katari.profiles.ui.switcher.ProfileSwitchEffects

/** Graph entry points of the profile picker, switcher and management screens, which have no view model to inject them. */
interface ProfilesGraph {
    val profileManager: ProfileManager
    val profilesPreferences: ProfilesPreferences
    val profileUnlock: ProfileUnlock
    val profileSwitchEffects: ProfileSwitchEffects

    /** Tells whether profiles can be deleted, which deletes their entries. */
    val entryDestructiveRemovalFeature: EntryDestructiveRemovalFeature
}
