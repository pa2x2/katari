package eu.kanade.tachiyomi.ui.library.update.report

import eu.kanade.presentation.util.Screen

/** The report of the last library update, opened from the library update settings. */
interface LibraryUpdateReportDestination {
    /** False where the report can't be shown yet; the last update is then not clickable. */
    val isAvailable: Boolean

    /** Only called while [isAvailable]. */
    fun screen(): Screen
}
