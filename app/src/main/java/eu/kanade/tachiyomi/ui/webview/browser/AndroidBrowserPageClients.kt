package eu.kanade.tachiyomi.ui.webview.browser

import android.graphics.Bitmap
import android.os.Message
import android.view.ViewGroup
import android.webkit.JsPromptResult
import android.webkit.JsResult
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebView
import com.kevinnzou.web.AccompanistWebChromeClient
import com.kevinnzou.web.AccompanistWebViewClient

/** Tells [page]'s events what its [WebView] loads, and keeps the source's headers to the source's site. */
internal class AndroidBrowserPageClient(private val page: AndroidBrowserPage) : AccompanistWebViewClient() {
    override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
        super.onPageStarted(view, url, favicon)
        url?.let { page.events.onUrlChange(page, it) }
    }

    override fun onPageFinished(view: WebView, url: String?) {
        super.onPageFinished(view, url)
        page.events.onPageFinished(page, url)
    }

    override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
        if (page.webView === view) page.webView = null
        (view.parent as? ViewGroup)?.removeView(view)
        view.destroy()
        page.events.onRenderProcessGone(detail.didCrash())
        return true
    }

    override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
        super.doUpdateVisitedHistory(view, url, isReload)
        url?.let { page.events.onUrlChange(page, it) }
    }

    override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
        val url = request?.url?.toString() ?: return false

        // Ignore intents urls
        if (url.startsWith("intent://")) return true

        // Only send source headers to the source itself
        if (request.url.host != page.siteHeaders.host) return false

        // Only open valid web urls
        if (url.startsWith("http") || url.startsWith("https")) {
            if (url != view?.url) {
                view?.loadUrl(url, page.siteHeaders.headers)
                return true
            }
        }

        return false
    }
}

/** Hands [page]'s popups and JavaScript dialogs to its events. */
internal class AndroidBrowserPageChromeClient(private val page: AndroidBrowserPage) : AccompanistWebChromeClient() {
    override fun onCreateWindow(view: WebView, isDialog: Boolean, isUserGesture: Boolean, resultMsg: Message): Boolean =
        page.events.onNewWindow(page.popup(resultMsg), isUserGesture)

    override fun onJsAlert(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
        page.events.onJsDialog(
            BrowserJsDialog.Alert(url, message.orEmpty(), onConfirm = result::confirm, onCancel = result::cancel),
        )
        return true
    }

    override fun onJsConfirm(view: WebView, url: String?, message: String?, result: JsResult): Boolean {
        page.events.onJsDialog(
            BrowserJsDialog.Confirm(url, message.orEmpty(), onConfirm = result::confirm, onCancel = result::cancel),
        )
        return true
    }

    override fun onJsPrompt(
        view: WebView,
        url: String?,
        message: String?,
        defaultValue: String?,
        result: JsPromptResult,
    ): Boolean {
        page.events.onJsDialog(
            BrowserJsDialog.Prompt(
                url = url,
                message = message.orEmpty(),
                defaultValue = defaultValue.orEmpty(),
                onConfirm = result::confirm,
                onCancel = result::cancel,
            ),
        )
        return true
    }
}
