package eu.kanade.tachiyomi.ui.library

import mihon.feature.profiles.core.ProfileManager
import tachiyomi.domain.library.service.LibraryUpdateQueue

interface LibraryGraph {
    val profileManager: ProfileManager
    val libraryUpdateQueue: LibraryUpdateQueue
}
