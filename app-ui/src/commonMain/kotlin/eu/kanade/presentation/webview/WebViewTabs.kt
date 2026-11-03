package eu.kanade.presentation.webview

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import eu.kanade.tachiyomi.ui.webview.browser.BrowserJsDialog
import eu.kanade.tachiyomi.ui.webview.browser.BrowserPage

/**
 * The web view screen's tabs: the page it opened and the popups opened over it, in the order they were last shown, the
 * last one on screen. Pages keep running in the background, so what a page reports goes to its own tab, and the shown
 * tab's bar and actions never take another page's address. A JavaScript dialog belongs to the tab that asked: the tab
 * comes forward to be answered, as a desktop browser activates a tab that shows a dialog, and dialogs from several tabs
 * wait their turn, each answered over its own tab.
 */
@Stable
internal class WebViewTabs {
    private val tabs = mutableStateListOf<WebViewTab>()

    // In the order asked; the first is the one on screen
    private val dialogs = mutableStateListOf<TabDialog>()

    private var closed = false

    /** Only read once a tab is open. */
    val shown: WebViewTab
        get() = tabs.last()

    val count: Int
        get() = tabs.size

    /** The dialog to answer now, over [shown], which is its tab; null when no page waits for an answer. */
    val dialog: BrowserJsDialog?
        get() = dialogs.firstOrNull()?.dialog

    /** Shows [page] over the tab shown so far; [url] is the address it loads, null where it isn't known yet. */
    fun open(page: BrowserPage, url: String?) {
        tabs += WebViewTab(page, url)
    }

    /** Closes the shown tab's page and shows the tab shown before it. */
    fun closeShown() {
        val tab = tabs.removeAt(tabs.lastIndex)
        dismissDialogsOf(tab)
        tab.page.close()
        dialogs.firstOrNull()?.let { bringForward(it.tab) }
    }

    /** Closes every page, as the screen closes; their dialogs are answered as the pages would be closing. */
    fun closeAll() {
        closed = true
        dialogs.forEach { it.dialog.answerUnseen() }
        dialogs.clear()
        tabs.forEach { it.page.close() }
    }

    fun onUrlChange(page: BrowserPage, url: String) {
        tabOf(page)?.url = url
    }

    fun onCloudflareCheck(page: BrowserPage, showsCheck: Boolean) {
        tabOf(page)?.showsCloudflareHelp = showsCheck
    }

    /** [page] waits for [dialog] to be answered; its tab comes forward once the dialogs asked before are answered. */
    fun onDialog(page: BrowserPage, dialog: BrowserJsDialog) {
        val tab = tabOf(page)?.takeIf { !closed }
        if (tab == null) {
            dialog.answerUnseen()
            return
        }
        dialogs += TabDialog(tab, dialog)
        if (dialogs.size == 1) bringForward(tab)
    }

    /** [dialog], the one on screen, has been answered; the next one asked brings its own tab forward. */
    fun onDialogAnswered(dialog: BrowserJsDialog) {
        dialogs.removeAll { it.dialog === dialog }
        dialogs.firstOrNull()?.let { bringForward(it.tab) }
    }

    private fun bringForward(tab: WebViewTab) {
        if (tabs.last() === tab) return
        tabs.remove(tab)
        tabs += tab
    }

    private fun dismissDialogsOf(tab: WebViewTab) {
        dialogs.filter { it.tab === tab }.forEach {
            it.dialog.answerUnseen()
            dialogs.remove(it)
        }
    }

    // A closed page may still report what it was doing as it closed
    private fun tabOf(page: BrowserPage) = tabs.firstOrNull { it.page === page }
}

@Stable
internal class WebViewTab(val page: BrowserPage, url: String?) {
    /** The address [page] last reported; null for a popup until it reports its first. */
    var url by mutableStateOf(url)

    var showsCloudflareHelp by mutableStateOf(false)
}

private class TabDialog(val tab: WebViewTab, val dialog: BrowserJsDialog)

// Answers a dialog the user can no longer see as a browser does when its page is closing
private fun BrowserJsDialog.answerUnseen() {
    when (this) {
        is BrowserJsDialog.Alert -> confirm()
        is BrowserJsDialog.Confirm, is BrowserJsDialog.Prompt -> cancel()
    }
}
