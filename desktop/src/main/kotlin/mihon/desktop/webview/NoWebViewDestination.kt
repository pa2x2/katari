package mihon.desktop.webview

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.webview.WebViewDestination
import mihon.core.platform.PlatformContext

/** Desktop has no web view, so shared screens don't offer to open a source's site in one. */
@Inject
class NoWebViewDestination : WebViewDestination {
    override val isAvailable: Boolean = false

    override fun screen(url: String, title: String?, sourceId: Long?, headers: Map<String, String>): Screen =
        error("The web view is not available on desktop")

    override fun open(context: PlatformContext, url: String, title: String?, sourceId: Long?) =
        error("The web view is not available on desktop")
}
