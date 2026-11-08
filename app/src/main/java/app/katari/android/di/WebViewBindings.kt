package app.katari.android.di

import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Binds
import eu.kanade.tachiyomi.ui.webview.browser.AndroidBrowserView
import eu.kanade.tachiyomi.ui.webview.browser.BrowserView

@BindingContainer
interface WebViewBindings {
    @Binds
    val AndroidBrowserView.browserView: BrowserView
}
