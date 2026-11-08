package eu.kanade.presentation.more.settings

import app.katari.profiles.core.ProfileStore

/** Graph entry points of the settings framework, which every settings screen is built on. */
interface PreferenceScreenGraph {
    /** Tells which rows' preferences are the active profile's, to mark them so. */
    val profileStore: ProfileStore
}
