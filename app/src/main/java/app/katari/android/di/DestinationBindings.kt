package app.katari.android.di

import app.katari.entry.interactions.source.EntryWebViewDestination
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import eu.kanade.tachiyomi.ui.webview.WebViewScreenDestination

@BindingContainer
interface DestinationBindings {
    @Binds
    val WebViewScreenDestination.webViewDestination: WebViewDestination

    @Binds
    val WebViewScreenDestination.entryWebViewDestination: EntryWebViewDestination
}
