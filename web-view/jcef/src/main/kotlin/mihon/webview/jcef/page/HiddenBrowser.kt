package mihon.webview.jcef.page

import org.cef.CefBrowserSettings
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefRendering
import java.awt.Component

/** The windowless Chromium browsers of the pages nobody sees, such as challenge pages and extensions' pages. */
internal object HiddenBrowser {
    /** The page such a browser starts on, which gives it a page its user agent is set on before the first load. */
    const val BLANK_PAGE = "about:blank"

    /** A browser of [client]'s, laid out as in a desktop window and never drawn ([UnpaintedView]). */
    fun create(client: CefClient): CefBrowser {
        // JCEF takes a browser to be shown in an AWT component; one in no window keeps AWT from acting for the page
        val anchor = object : Component() {}
        val browser = client.createBrowser(
            BLANK_PAGE,
            CefRendering.CefRenderingWithHandler(UnpaintedView(), anchor),
            false,
            null,
            CefBrowserSettings(),
        )
        // Otherwise Chromium waits for the app to agree to the browser closing, and its shutdown waits for it
        browser.setCloseAllowed()
        return browser
    }
}
