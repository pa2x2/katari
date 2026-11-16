package app.katari.webview.jcef.surface

import app.katari.webview.engine.opener.OpenerMessage
import app.katari.webview.engine.opener.OpenerScript
import kotlinx.coroutines.launch

/**
 * The pages [page] is linked with as opener and popup ([OpenerScript]): its [opener], where `window.open` opened it,
 * the popups it opened that way, and the pages at the other end of the message ports its messages took. Only on the
 * main thread.
 */
internal class PageWindows(private val page: ChromiumPage, private val opener: Opener?) {
    val script = OpenerScript()

    /** The script for the start of every document of the page's. */
    val documentScript: String
        get() = script.source(hasOpener = opener != null)

    private val popups = HashMap<String, ChromiumPage>()

    // The page at the other end of each port, by the port's id
    private val ports = HashMap<String, ChromiumPage>()

    private var closed = false

    fun on(message: OpenerMessage) {
        if (closed) return
        when (message) {
            is OpenerMessage.Open -> open(message)
            is OpenerMessage.Post -> {
                val (target, window) = if (message.window == null) {
                    opener?.let { it.page to it.window } ?: return
                } else {
                    (popups[message.window] ?: return) to null
                }
                message.ports.forEach { port ->
                    ports[port] = target
                    target.windows.ports[port] = page
                }
                target.windows.deliver(target.windows.script.deliver(message, window))
            }
            is OpenerMessage.PortPost -> ports[message.port]?.let {
                it.windows.deliver(it.windows.script.deliver(message))
            }
            is OpenerMessage.Close -> {
                val window = if (message.window == null) page.takeIf { opener != null } else popups[message.window]
                window?.let(page.events::onCloseWindow)
            }
        }
    }

    /** The page closed; the pages linked with it learn that it did. */
    fun onClosed() {
        if (closed) return
        closed = true
        opener?.let { it.page.windows.onPopupClosed(it.window) }
        popups.values.forEach { it.windows.onOpenerClosed() }
        popups.clear()
        ports.clear()
    }

    private fun open(message: OpenerMessage.Open) {
        popups[message.window]?.let {
            it.load(message.url)
            return
        }
        val popup = page.popup(message.url, Opener(page, message.window))
        // A popup the screen refuses never opened its browser, so it needs no closing
        if (page.events.onNewWindow(popup, page.input.isUserGesture())) {
            popups[message.window] = popup
        } else {
            deliver(script.deliverClosed(message.window))
        }
    }

    private fun onPopupClosed(window: String) {
        if (popups.remove(window) != null && !closed) deliver(script.deliverClosed(window))
    }

    private fun onOpenerClosed() {
        if (!closed) deliver(script.deliverOpenerClosed())
    }

    // Chromium runs the scripts in the order they were sent
    private fun deliver(script: String) {
        page.scope.launch { page.evaluateJavascript(script) }
    }

    /** The page that opened a popup, and the id its script knows the popup by. */
    class Opener(val page: ChromiumPage, val window: String)
}
