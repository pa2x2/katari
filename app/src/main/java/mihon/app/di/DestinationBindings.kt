package mihon.app.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.BrowseLongPressActionsScreenDestination
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageDestination
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageScreenDestination
import eu.kanade.presentation.more.settings.screen.browse.longpress.BrowseLongPressActionsDestination
import eu.kanade.tachiyomi.ui.browse.BrowseDestination
import eu.kanade.tachiyomi.ui.browse.BrowseScreenDestination
import eu.kanade.tachiyomi.ui.entry.EntryDetailsDestination
import eu.kanade.tachiyomi.ui.entry.EntryScreenDestination
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import eu.kanade.tachiyomi.ui.webview.WebViewScreenDestination
import mihon.feature.migration.EntryMigrationDestination
import mihon.feature.migration.config.MigrationConfigScreenDestination

@BindingContainer
interface DestinationBindings {
    @Binds
    val EntryScreenDestination.entryDetailsDestination: EntryDetailsDestination

    @Binds
    val AppLanguageScreenDestination.appLanguageDestination: AppLanguageDestination

    @Binds
    val BrowseLongPressActionsScreenDestination.browseLongPressActionsDestination: BrowseLongPressActionsDestination

    @Binds
    val BrowseScreenDestination.browseDestination: BrowseDestination

    @Binds
    val MigrationConfigScreenDestination.entryMigrationDestination: EntryMigrationDestination

    @Binds
    val WebViewScreenDestination.webViewDestination: WebViewDestination
}
