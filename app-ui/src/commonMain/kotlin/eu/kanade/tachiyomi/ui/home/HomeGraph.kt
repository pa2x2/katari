package eu.kanade.tachiyomi.ui.home

import app.katari.profiles.core.ProfileManager
import app.katari.profiles.core.ProfileStore

/** Graph services the home screen reads; it has no view model of its own. */
interface HomeGraph {
    val profileManager: ProfileManager
    val profileStore: ProfileStore
}
