package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageDestination
import eu.kanade.tachiyomi.ui.browse.BrowseDestination
import eu.kanade.tachiyomi.ui.browse.source.preferences.SourcePreferencesDestination
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import mihon.desktop.appearance.NoAppLanguageDestination
import mihon.desktop.browse.NoBrowseDestination
import mihon.desktop.browse.NoSourcePreferencesDestination
import mihon.desktop.migration.NoEntryMigrationDestination
import mihon.desktop.webview.NoWebViewDestination
import mihon.feature.migration.EntryMigrationDestination

@BindingContainer
interface DestinationBindings {
    @Binds
    val NoAppLanguageDestination.appLanguageDestination: AppLanguageDestination

    @Binds
    val NoBrowseDestination.browseDestination: BrowseDestination

    @Binds
    val NoSourcePreferencesDestination.sourcePreferencesDestination: SourcePreferencesDestination

    @Binds
    val NoEntryMigrationDestination.entryMigrationDestination: EntryMigrationDestination

    @Binds
    val NoWebViewDestination.webViewDestination: WebViewDestination
}
