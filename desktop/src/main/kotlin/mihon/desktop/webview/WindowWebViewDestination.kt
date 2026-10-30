package mihon.desktop.webview

import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import eu.kanade.tachiyomi.ui.webview.WebViewScreen
import mihon.core.platform.PlatformContext
import mihon.desktop.window.WindowPlatformContext

/** The web view as a screen of the window's navigation, the one a call from outside a screen pushes it on. */
class WindowWebViewDestination : WebViewDestination {
    override val isAvailable: Boolean = true

    override fun screen(url: String, title: String?, sourceId: Long?, headers: Map<String, String>): Screen =
        WebViewScreen(url = url, initialTitle = title, sourceId = sourceId, headers = headers)

    override fun open(context: PlatformContext, url: String, title: String?, sourceId: Long?) {
        val navigator = (context as? WindowPlatformContext)?.navigator
            ?: error("The web view opens only from a window that shows the app's screens")
        navigator.push(screen(url, title, sourceId, headers = emptyMap()))
    }
}
