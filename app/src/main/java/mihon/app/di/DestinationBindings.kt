package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.BrowseLongPressActionsScreenDestination
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageDestination
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageScreenDestination
import eu.kanade.presentation.more.settings.screen.browse.longpress.BrowseLongPressActionsDestination
import eu.kanade.presentation.more.settings.screen.navigation.HomeNavigationEditorDestination
import eu.kanade.presentation.more.settings.screen.navigation.HomeNavigationEditorScreenDestination
import eu.kanade.presentation.more.settings.screen.textrules.BookTextRulesDestination
import eu.kanade.presentation.more.settings.screen.textrules.BookTextRulesScreenDestination
import eu.kanade.tachiyomi.ui.entry.EntryDetailsDestination
import eu.kanade.tachiyomi.ui.entry.EntryScreenDestination
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityDestination
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityScreenDestination
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportDestination
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportScreenDestination

@BindingContainer
interface DestinationBindings {
    @Binds
    val EntryScreenDestination.entryDetailsDestination: EntryDetailsDestination

    @Binds
    val HistoryActivityScreenDestination.historyActivityDestination: HistoryActivityDestination

    @Binds
    val AppLanguageScreenDestination.appLanguageDestination: AppLanguageDestination

    @Binds
    val HomeNavigationEditorScreenDestination.homeNavigationEditorDestination: HomeNavigationEditorDestination

    @Binds
    val BrowseLongPressActionsScreenDestination.browseLongPressActionsDestination: BrowseLongPressActionsDestination

    @Binds
    val LibraryUpdateReportScreenDestination.libraryUpdateReportDestination: LibraryUpdateReportDestination

    @Binds
    val BookTextRulesScreenDestination.bookTextRulesDestination: BookTextRulesDestination
}
