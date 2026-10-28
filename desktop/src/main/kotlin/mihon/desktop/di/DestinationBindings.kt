package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageDestination
import eu.kanade.presentation.more.settings.screen.browse.longpress.BrowseLongPressActionsDestination
import eu.kanade.tachiyomi.ui.entry.EntryDetailsDestination
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportDestination
import mihon.desktop.appearance.NoAppLanguageDestination
import mihon.desktop.browse.NoBrowseLongPressActionsDestination
import mihon.desktop.entry.NoEntryDetailsDestination
import mihon.desktop.library.NoLibraryUpdateReportDestination

@BindingContainer
interface DestinationBindings {
    @Binds
    val NoEntryDetailsDestination.entryDetailsDestination: EntryDetailsDestination

    @Binds
    val NoAppLanguageDestination.appLanguageDestination: AppLanguageDestination

    @Binds
    val NoBrowseLongPressActionsDestination.browseLongPressActionsDestination: BrowseLongPressActionsDestination

    @Binds
    val NoLibraryUpdateReportDestination.libraryUpdateReportDestination: LibraryUpdateReportDestination
}
