package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.presentation.more.settings.screen.appearance.AppLanguageDestination
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import mihon.desktop.appearance.NoAppLanguageDestination
import mihon.desktop.webview.NoWebViewDestination

@BindingContainer
interface DestinationBindings {
    @Binds
    val NoAppLanguageDestination.appLanguageDestination: AppLanguageDestination

    @Binds
    val NoWebViewDestination.webViewDestination: WebViewDestination
}
