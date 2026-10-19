package eu.kanade.tachiyomi.data.library

import mihon.feature.profiles.core.ProfileDatabase
import mihon.feature.profiles.core.ProfileStore
import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

/** Graph entry points of the library update jobs, which WorkManager constructs, and of their scheduling. */
interface LibraryUpdateGraph {
    fun inject(job: LibraryUpdateJob)

    fun inject(job: MetadataUpdateJob)

    val libraryUpdateRulesRepository: LibraryUpdateRulesRepository

    val activeProfileProvider: ActiveProfileProvider

    val profileStore: ProfileStore

    val profileDatabase: ProfileDatabase
}
