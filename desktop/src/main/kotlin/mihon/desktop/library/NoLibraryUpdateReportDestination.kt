package mihon.desktop.library

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportDestination

/**
 * Desktop can't show the library update report yet, so the last update in the library update settings doesn't open it.
 * Stands in until desktop hosts the report screen.
 */
@Inject
class NoLibraryUpdateReportDestination : LibraryUpdateReportDestination {
    override val isAvailable: Boolean = false

    override fun screen(): Screen = error("The library update report is not available on desktop")
}
