package eu.kanade.tachiyomi.ui.home

import mihon.feature.profiles.core.ProfileManager
import mihon.feature.profiles.core.ProfileStore

/** Graph services the home screen reads; it has no view model of its own. */
interface HomeGraph {
    val profileManager: ProfileManager
    val profileStore: ProfileStore
}
