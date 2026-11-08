package app.katari.webview.jcef.surface

import eu.kanade.tachiyomi.ui.webview.browser.BrowserJsDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.cef.CefClient
import org.cef.browser.CefBrowser
import org.cef.browser.CefFrame
import org.cef.callback.CefFileDialogCallback
import org.cef.callback.CefJSDialogCallback
import org.cef.handler.CefDialogHandler
import org.cef.handler.CefDisplayHandlerAdapter
import org.cef.handler.CefJSDialogHandler
import org.cef.handler.CefJSDialogHandlerAdapter
import org.cef.handler.CefLifeSpanHandlerAdapter
import org.cef.handler.CefLoadHandlerAdapter
import org.cef.handler.CefRequestHandler
import org.cef.handler.CefRequestHandlerAdapter
import org.cef.handler.CefResourceRequestHandler
import org.cef.misc.BoolRef
import org.cef.network.CefRequest
import java.util.Vector
import javax.swing.SwingUtilities

/**
 * What Chromium tells of [page]'s browser, on threads of its own, brought to the page and the screen's events on the
 * main thread. Chromium's own windows stay closed: a new window opens as a page of the screen's, JavaScript dialogs
 * are the screen's, and file choosers and downloads are refused, as Android's web view refuses them without an app that
 * handles them.
 */
internal class PageClient(private val page: ChromiumPage) {
    fun attachTo(client: CefClient) {
        client.addLifeSpanHandler(lifeSpan)
        client.addLoadHandler(load)
        client.addDisplayHandler(display)
        client.addRequestHandler(requests)
        client.addJSDialogHandler(jsDialogs)
        client.addDialogHandler(fileDialogs)
        client.addKeyboardHandler(page.input.keyboardHandler)
        client.addContextMenuHandler(page.input.contextMenuHandler)
        client.addFocusHandler(page.input.focusHandler)
    }

    private val lifeSpan = object : CefLifeSpanHandlerAdapter() {
        override fun onAfterCreated(browser: CefBrowser) {
            onMainThread { page.onCreated(browser) }
        }

        override fun onBeforeClose(browser: CefBrowser) {
            // The page is gone from the screen by now, but its client is still to be freed
            SwingUtilities.invokeLater { page.onClosed(browser) }
        }

        // Chromium would open the window itself, outside the app; the screen opens it as a page instead
        override fun onBeforePopup(
            browser: CefBrowser,
            frame: CefFrame?,
            targetUrl: String?,
            targetFrameName: String?,
        ): Boolean {
            if (!targetUrl.isNullOrEmpty()) openPopup(targetUrl, page.input.isUserGesture())
            return true
        }
    }

    private val load = object : CefLoadHandlerAdapter() {
        override fun onLoadingStateChange(
            browser: CefBrowser,
            isLoading: Boolean,
            canGoBack: Boolean,
            canGoForward: Boolean,
        ) {
            onMainThread { page.onLoadingStateChange(isLoading, canGoBack, canGoForward) }
        }

        // Frames of other sites, such as a challenge's, keep storage of their own
        override fun onLoadStart(browser: CefBrowser, frame: CefFrame, transitionType: CefRequest.TransitionType) {
            frame.url?.let(page.origins::loaded)
        }

        override fun onLoadEnd(browser: CefBrowser, frame: CefFrame, httpStatusCode: Int) {
            page.cookies.readBack()
            if (!frame.isMain) return
            val url = frame.url
            onMainThread { if (page.showsSite) page.events.onPageFinished(page, url) }
        }
    }

    private val display = object : CefDisplayHandlerAdapter() {
        override fun onAddressChange(browser: CefBrowser, frame: CefFrame, url: String) {
            page.origins.loaded(url)
            page.cookies.readBack()
            if (frame.isMain) onMainThread { if (page.onAddressChange(url)) page.events.onUrlChange(page, url) }
        }

        override fun onTitleChange(browser: CefBrowser, title: String) {
            onMainThread { page.onTitleChange(title) }
        }

        override fun onTooltip(browser: CefBrowser, text: String?): Boolean {
            onMainThread { page.input.showTooltip(text) }
            return true
        }
    }

    private val requests = object : CefRequestHandlerAdapter() {
        override fun getResourceRequestHandler(
            browser: CefBrowser,
            frame: CefFrame?,
            request: CefRequest,
            isNavigation: Boolean,
            isDownload: Boolean,
            requestInitiator: String?,
            disableDefaultHandling: BoolRef,
        ): CefResourceRequestHandler? = page.siteHeaders.handlerFor(request)

        // A link opened in a new tab, as by a middle click, opens as a page of the screen's, as a new window does
        override fun onOpenURLFromTab(
            browser: CefBrowser,
            frame: CefFrame?,
            targetUrl: String?,
            userGesture: Boolean,
        ): Boolean {
            if (!targetUrl.isNullOrEmpty()) openPopup(targetUrl, userGesture)
            return true
        }

        override fun onRenderProcessTerminated(
            browser: CefBrowser,
            status: CefRequestHandler.TerminationStatus,
            errorCode: Int,
            errorString: String?,
        ) {
            // The system ending a renderer for its memory isn't the page crashing
            val crashed = status != CefRequestHandler.TerminationStatus.TS_PROCESS_OOM &&
                status != CefRequestHandler.TerminationStatus.TS_PROCESS_WAS_KILLED
            onMainThread { page.events.onRenderProcessGone(crashed) }
        }
    }

    private val jsDialogs = object : CefJSDialogHandlerAdapter() {
        override fun onJSDialog(
            browser: CefBrowser,
            originUrl: String?,
            dialogType: CefJSDialogHandler.JSDialogType,
            messageText: String?,
            defaultPromptText: String?,
            callback: CefJSDialogCallback,
            suppressMessage: BoolRef,
        ): Boolean {
            val message = messageText.orEmpty()
            val cancel = { callback.Continue(false, "") }
            val dialog = when (dialogType) {
                CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_ALERT ->
                    BrowserJsDialog.Alert(originUrl, message, onConfirm = { callback.Continue(true, "") }, cancel)
                CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_CONFIRM ->
                    BrowserJsDialog.Confirm(originUrl, message, onConfirm = { callback.Continue(true, "") }, cancel)
                CefJSDialogHandler.JSDialogType.JSDIALOGTYPE_PROMPT -> BrowserJsDialog.Prompt(
                    url = originUrl,
                    message = message,
                    defaultValue = defaultPromptText.orEmpty(),
                    onConfirm = { callback.Continue(true, it) },
                    onCancel = cancel,
                )
            }
            showDialog(dialog)
            return true
        }

        // A page that asks before it is left, as on following a link away from an unsaved form, asks through the
        // screen's confirmation; closing the screen closes the page without asking, as Android's web view does
        override fun onBeforeUnloadDialog(
            browser: CefBrowser,
            messageText: String?,
            isReload: Boolean,
            callback: CefJSDialogCallback,
        ): Boolean {
            showDialog(
                BrowserJsDialog.Confirm(
                    url = browser.url,
                    message = messageText.orEmpty(),
                    onConfirm = { callback.Continue(true, "") },
                    onCancel = { callback.Continue(false, "") },
                ),
            )
            return true
        }
    }

    private val fileDialogs = object : CefDialogHandler {
        override fun onFileDialog(
            browser: CefBrowser,
            mode: CefDialogHandler.FileDialogMode,
            title: String?,
            defaultFilePath: String?,
            acceptFilters: Vector<String>?,
            acceptExtensions: Vector<String>?,
            acceptDescriptions: Vector<String>?,
            callback: CefFileDialogCallback,
        ): Boolean {
            callback.Cancel()
            return true
        }
    }

    private fun openPopup(url: String, isUserGesture: Boolean) = onMainThread {
        // A popup the screen refuses never opened its browser, so it needs no closing
        page.events.onNewWindow(page.popup(url), isUserGesture)
    }

    private fun showDialog(dialog: BrowserJsDialog) {
        SwingUtilities.invokeLater { page.events.onJsDialog(page, dialog) }
    }

    // The screen's events stop with the screen
    private fun onMainThread(block: suspend CoroutineScope.() -> Unit) {
        page.scope.launch(block = block)
    }
}
