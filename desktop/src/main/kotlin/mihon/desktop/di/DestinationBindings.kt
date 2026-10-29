package mihon.desktop.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import mihon.desktop.webview.NoWebViewDestination

@BindingContainer
interface DestinationBindings {
    @Binds
    val NoWebViewDestination.webViewDestination: WebViewDestination
}
