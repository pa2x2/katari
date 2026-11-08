package app.katari.desktop.webview

import app.katari.core.common.web.ExtensionPage
import app.katari.core.common.web.ExtensionPageEvents
import app.katari.core.common.web.ExtensionPageSettings
import app.katari.core.common.web.ExtensionPages

/**
 * Without JCEF in the Java runtime the app runs on, desktop has no pages for extensions' web views, so an extension's
 * `WebView` fails as it does wherever Android lacks one, and the user is told the source needs a web view.
 */
class NoExtensionPages(private val userAgent: () -> String) : ExtensionPages {
    override val isOffered: Boolean = false

    override val defaultUserAgent: String
        get() = userAgent()

    override fun open(settings: ExtensionPageSettings, events: ExtensionPageEvents): ExtensionPage =
        error("Extension pages need a Java runtime with JCEF")
}
