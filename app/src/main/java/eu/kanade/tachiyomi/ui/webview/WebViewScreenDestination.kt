package eu.kanade.tachiyomi.ui.webview

import app.katari.core.platform.PlatformContext
import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen

/** The web view as a screen in the main window, and as its own activity over a reader or player. */
@Inject
class WebViewScreenDestination : WebViewDestination {
    override val isAvailable: Boolean = true

    override fun screen(url: String, title: String?, sourceId: Long?, headers: Map<String, String>): Screen =
        WebViewScreen(url = url, initialTitle = title, sourceId = sourceId, headers = headers)

    override fun open(context: PlatformContext, url: String, title: String?, sourceId: Long?) {
        context.startActivity(WebViewActivity.newIntent(context, url, sourceId, title))
    }
}
