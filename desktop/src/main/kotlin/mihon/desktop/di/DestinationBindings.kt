package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageDestination
import eu.kanade.presentation.more.settings.screen.browse.longpress.BrowseLongPressActionsDestination
import eu.kanade.tachiyomi.ui.entry.EntryDetailsDestination
import eu.kanade.tachiyomi.ui.history.activity.HistoryActivityDestination
import eu.kanade.tachiyomi.ui.library.update.report.LibraryUpdateReportDestination
import eu.kanade.tachiyomi.ui.more.OnboardingDestination
import mihon.desktop.appearance.NoAppLanguageDestination
import mihon.desktop.browse.NoBrowseLongPressActionsDestination
import mihon.desktop.entry.NoEntryDetailsDestination
import mihon.desktop.history.NoHistoryActivityDestination
import mihon.desktop.library.NoLibraryUpdateReportDestination
import mihon.desktop.onboarding.NoOnboardingDestination

@BindingContainer
interface DestinationBindings {
    @Binds
    val NoEntryDetailsDestination.entryDetailsDestination: EntryDetailsDestination

    @Binds
    val NoHistoryActivityDestination.historyActivityDestination: HistoryActivityDestination

    @Binds
    val NoAppLanguageDestination.appLanguageDestination: AppLanguageDestination

    @Binds
    val NoBrowseLongPressActionsDestination.browseLongPressActionsDestination: BrowseLongPressActionsDestination

    @Binds
    val NoLibraryUpdateReportDestination.libraryUpdateReportDestination: LibraryUpdateReportDestination

    @Binds
    val NoOnboardingDestination.onboardingDestination: OnboardingDestination
}
