package eu.kanade.tachiyomi.data.library

import mihon.feature.profiles.core.ProfileStore
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.service.LibraryUpdateQueue

/** Graph entry points of the library update jobs, which WorkManager constructs. */
interface LibraryUpdateGraph {
    fun inject(job: LibraryUpdateJob)

    fun inject(job: MetadataUpdateJob)

    val libraryUpdateQueue: LibraryUpdateQueue

    val activeProfileProvider: ActiveProfileProvider

    val profileStore: ProfileStore
}
