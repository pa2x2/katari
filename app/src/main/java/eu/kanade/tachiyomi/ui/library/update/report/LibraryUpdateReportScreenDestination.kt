package eu.kanade.tachiyomi.ui.library.update.report

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen

@Inject
class LibraryUpdateReportScreenDestination : LibraryUpdateReportDestination {
    override val isAvailable: Boolean = true

    override fun screen(): Screen = LibraryUpdateReportScreen()
}
