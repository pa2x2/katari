package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageDestination
import eu.kanade.presentation.more.settings.screen.browse.longpress.BrowseLongPressActionsDestination
import eu.kanade.presentation.more.settings.screen.navigation.HomeNavigationEditorDestination
import eu.kanade.tachiyomi.ui.entry.EntryDetailsDestination
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityDestination
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportDestination
import mihon.desktop.appearance.NoAppLanguageDestination
import mihon.desktop.browse.NoBrowseLongPressActionsDestination
import mihon.desktop.entry.NoEntryDetailsDestination
import mihon.desktop.history.NoHistoryActivityDestination
import mihon.desktop.home.NoHomeNavigationEditorDestination
import mihon.desktop.library.NoLibraryUpdateReportDestination

@BindingContainer
interface DestinationBindings {
    @Binds
    val NoEntryDetailsDestination.entryDetailsDestination: EntryDetailsDestination

    @Binds
    val NoHistoryActivityDestination.historyActivityDestination: HistoryActivityDestination

    @Binds
    val NoAppLanguageDestination.appLanguageDestination: AppLanguageDestination

    @Binds
    val NoHomeNavigationEditorDestination.homeNavigationEditorDestination: HomeNavigationEditorDestination

    @Binds
    val NoBrowseLongPressActionsDestination.browseLongPressActionsDestination: BrowseLongPressActionsDestination

    @Binds
    val NoLibraryUpdateReportDestination.libraryUpdateReportDestination: LibraryUpdateReportDestination
}
