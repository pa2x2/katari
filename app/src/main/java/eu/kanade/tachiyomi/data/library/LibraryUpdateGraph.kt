package eu.kanade.tachiyomi.data.library

import tachiyomi.data.ActiveProfileProvider
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.library.update.repository.LibraryUpdateRulesRepository

/** Graph entry points of the library update jobs, which WorkManager constructs, and of their scheduling. */
interface LibraryUpdateGraph {
    fun inject(job: LibraryUpdateJob)

    fun inject(job: MetadataUpdateJob)

    val libraryPreferences: LibraryPreferences

    val libraryUpdateRulesRepository: LibraryUpdateRulesRepository

    val activeProfileProvider: ActiveProfileProvider
}
