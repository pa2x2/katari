package eu.kanade.presentation.more.settings.screen

import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportDestination
import tachiyomi.domain.category.interactor.GetCategories
import tachiyomi.domain.category.interactor.ResetCategoryFlags
import tachiyomi.domain.library.service.GlobalLibraryPreferences
import tachiyomi.domain.library.service.LibraryPreferences

/** Graph entry points of the library and library update settings. */
interface LibrarySettingsGraph {
    val getCategories: GetCategories
    val libraryPreferences: LibraryPreferences
    val globalLibraryPreferences: GlobalLibraryPreferences
    val resetCategoryFlags: ResetCategoryFlags
    val libraryUpdateReportDestination: LibraryUpdateReportDestination
}
