package eu.kanade.tachiyomi.ui.webview.browser

import android.content.pm.ApplicationInfo
import android.os.Message
import android.webkit.WebView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.net.toUri
import com.kevinnzou.web.WebContent
import dev.zacsweers.metro.Inject
import eu.kanade.tachiyomi.BuildConfig
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import eu.kanade.tachiyomi.util.system.setUserAgent
import kotlinx.coroutines.CoroutineScope
import com.kevinnzou.web.WebView as ComposeWebView

/** The system [WebView], through compose-webview. */
@Inject
class AndroidBrowserView : BrowserView {
    override fun openPage(
        request: BrowserRequest,
        userAgent: String,
        scope: CoroutineScope,
        events: BrowserEvents,
    ): BrowserPage = AndroidBrowserPage(
        content = WebContent.Url(url = request.url, additionalHttpHeaders = request.headers),
        popupMessage = null,
        siteHeaders = SiteHeaders(request.url.toUri().host, request.headers),
        userAgent = userAgent,
        scope = scope,
        events = events,
    )

    @Composable
    override fun Page(page: BrowserPage, modifier: Modifier) {
        require(page is AndroidBrowserPage) { "A page this browser didn't open" }
        ComposeWebView(
            state = page.state,
            modifier = modifier,
            // The screen goes back through the page's history itself, before it closes a tab
            captureBackPresses = false,
            navigator = page.navigator,
            onCreated = { webView ->
                webView.setDefaultSettings()

                // Debug mode (chrome://inspect/#devices)
                if (BuildConfig.DEBUG &&
                    0 != webView.context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE
                ) {
                    WebView.setWebContentsDebuggingEnabled(true)
                }

                webView.setUserAgent(page.userAgent)
            },
            onDispose = page::onHidden,
            client = page.client,
            chromeClient = page.chromeClient,
            factory = { context ->
                (page.webView ?: WebView(context).also { webView -> page.popupMessage?.let { webView.adopt(it) } })
                    .also(page::onShown)
            },
        )
    }
}

// A popup's WebView reaches the page that opened it through the message its creation was asked with
private fun WebView.adopt(message: Message) {
    (message.obj as WebView.WebViewTransport).webView = this
    message.sendToTarget()
}
