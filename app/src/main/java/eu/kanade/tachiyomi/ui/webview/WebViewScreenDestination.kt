package eu.kanade.tachiyomi.ui.webview

import dev.zacsweers.metro.Inject
import eu.kanade.presentation.util.Screen

@Inject
class WebViewScreenDestination : WebViewDestination {
    override val isAvailable: Boolean = true

    override fun screen(url: String, title: String?, sourceId: Long?, headers: Map<String, String>): Screen =
        WebViewScreen(url = url, initialTitle = title, sourceId = sourceId, headers = headers)
}
