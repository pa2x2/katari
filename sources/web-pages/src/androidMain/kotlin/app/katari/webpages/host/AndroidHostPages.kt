package app.katari.webpages.host

import android.content.Context
import androidx.webkit.WebViewFeature
import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import eu.kanade.tachiyomi.source.entry.webpage.WebPageException
import eu.kanade.tachiyomi.util.system.WebViewUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Sources' pages in the system WebView: web views that belong to no window, which run their pages all the same, also
 * from background work with the screen off.
 *
 * A WebView too old to run a script before a document's own, or to hand a page's posts over, opens none: without those
 * a source would miss responses it waits for, with nothing telling why.
 */
class AndroidHostPages(
    private val context: Context,
    private val defaultUserAgent: () -> String,
    private val messages: WebViewInterceptorMessages,
) : HostPages {
    override suspend fun open(settings: HostPageSettings, events: HostPageEvents): HostPage {
        if (!WebViewUtil.supportsWebView(context)) throw WebPageException(messages.webViewRequired())
        if (!REQUIRED_FEATURES.all(WebViewFeature::isFeatureSupported)) {
            throw WebPageException(messages.webViewOutdated())
        }
        // A web view belongs to the thread it was made on, and Android's own work on it runs on the main one
        return withContext(Dispatchers.Main.immediate) {
            AndroidHostPage(context, settings, settings.userAgent ?: defaultUserAgent(), events)
        }
    }

    private companion object {
        val REQUIRED_FEATURES = listOf(WebViewFeature.DOCUMENT_START_SCRIPT, WebViewFeature.WEB_MESSAGE_LISTENER)
    }
}
