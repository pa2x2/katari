package app.katari.desktop.reader

import app.katari.entry.interactions.reader.settings.BookDocumentReaderSettings
import app.katari.entry.interactions.reader.window.BookReaderNavigation
import cafe.adriel.voyager.navigator.Navigator
import eu.kanade.tachiyomi.ui.setting.SettingsScreen
import eu.kanade.tachiyomi.ui.webview.WebViewDestination

/**
 * Where [screen]'s book reader leaves for: its settings and the web view open over it, as Android's open over the
 * reader's activity.
 */
internal class WindowBookReaderNavigation(
    private val navigator: Navigator,
    private val screen: BookReaderScreen,
    private val webViewDestination: WebViewDestination,
) : BookReaderNavigation {
    override val webViewAvailable: Boolean get() = webViewDestination.isAvailable

    override fun close() {
        if (navigator.lastItem === screen) navigator.pop()
    }

    override fun openReaderSettings() {
        navigator.push(
            SettingsScreen(
                destination = SettingsScreen.Destination.Readers,
                viewerSettingsSurfaceId = BookDocumentReaderSettings.SURFACE_ID,
                viewerSettingsHighlight = null,
            ),
        )
    }

    override fun openWebView(url: String, title: String?, sourceId: Long?) {
        navigator.push(webViewDestination.screen(url, title, sourceId, headers = emptyMap()))
    }
}
