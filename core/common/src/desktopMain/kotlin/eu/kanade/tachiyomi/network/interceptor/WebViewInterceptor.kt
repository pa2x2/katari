package eu.kanade.tachiyomi.network.interceptor

import android.content.Context
import android.webkit.WebView
import android.widget.Toast
import eu.kanade.tachiyomi.util.system.WebViewUtil
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import eu.kanade.tachiyomi.util.system.setUserAgent
import eu.kanade.tachiyomi.util.system.toast
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Android's interceptor for responses only a web view gets through, kept for extensions that subclass it. Extensions
 * can't build a web view on desktop yet, so theirs behave as on an Android device without one: such a response is passed
 * on as it is, and the user is told the source needs a web view. [CloudflareInterceptor] solves in pages of the app's
 * own, where the app has them.
 */
abstract class WebViewInterceptor(
    private val context: Context,
    private val defaultUserAgentProvider: () -> String,
    protected val messages: WebViewInterceptorMessages,
) : Interceptor {

    abstract fun shouldIntercept(response: Response): Boolean

    abstract fun intercept(chain: Interceptor.Chain, request: Request, response: Response): Response

    protected open fun prepareRequest(request: Request): Request = request

    /** Whether there is a web view to get through with; the app's own interceptors ask their pages instead. */
    internal open fun offersWebView(): Boolean = WebViewUtil.supportsWebView(context)

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = prepareRequest(chain.request())
        val response = chain.proceed(request)
        if (!shouldIntercept(response)) {
            return response
        }

        if (!offersWebView()) {
            context.toast(messages.webViewRequired(), Toast.LENGTH_LONG)
            return response
        }

        return intercept(chain, request, response)
    }

    fun CountDownLatch.awaitFor30Seconds() {
        await(30, TimeUnit.SECONDS)
    }

    /** Throws the stand-in `WebView`'s "Stub!" until desktop has a web view. */
    fun createWebView(request: Request): WebView {
        return WebView(context).apply {
            setDefaultSettings()
            // Avoid sending empty User-Agent, Chromium WebView will reset to default if empty
            val userAgent = request.header("User-Agent")
                ?.takeIf { it.isNotEmpty() }
                ?: defaultUserAgentProvider()
            setUserAgent(userAgent)
        }
    }
}
