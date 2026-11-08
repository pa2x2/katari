package app.katari.desktop.webview

import app.katari.core.platform.PlatformContext
import eu.kanade.presentation.util.Screen
import eu.kanade.tachiyomi.ui.webview.WebViewDestination

/**
 * Without JCEF in the Java runtime the app runs on, desktop has no web view, so shared screens don't offer to open a
 * source's site in one.
 */
class NoWebViewDestination : WebViewDestination {
    override val isAvailable: Boolean = false

    override fun screen(url: String, title: String?, sourceId: Long?, headers: Map<String, String>): Screen =
        error("The web view needs a Java runtime with JCEF")

    override fun open(context: PlatformContext, url: String, title: String?, sourceId: Long?) =
        error("The web view needs a Java runtime with JCEF")
}
