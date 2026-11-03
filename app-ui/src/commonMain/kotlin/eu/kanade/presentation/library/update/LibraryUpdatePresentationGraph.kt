package eu.kanade.presentation.library.update

import tachiyomi.domain.library.update.repository.LibraryUpdateReportRepository

/** Graph entry points of the library update starter. */
interface LibraryUpdatePresentationGraph {
    val libraryUpdateReportRepository: LibraryUpdateReportRepository
}
