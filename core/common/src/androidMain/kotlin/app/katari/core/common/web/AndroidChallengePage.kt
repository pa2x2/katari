package app.katari.core.common.web

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.webkit.JavascriptInterface
import android.webkit.RenderProcessGoneDetail
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.webkit.ScriptHandler
import androidx.webkit.WebViewCompat
import androidx.webkit.WebViewFeature
import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import eu.kanade.tachiyomi.util.system.WebViewUtil
import eu.kanade.tachiyomi.util.system.isOutdated
import eu.kanade.tachiyomi.util.system.setDefaultSettings
import eu.kanade.tachiyomi.util.system.setUserAgent
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import okhttp3.HttpUrl
import tachiyomi.core.common.util.system.logcat

/**
 * Challenge pages in an off-screen [WebView] made, driven and destroyed on the main thread, which a caller waiting for
 * the page therefore can't be on. The challenge's messages reach the app through a bridge in a script world of its
 * own, out of the page's reach, where the WebView has one, and through a JavaScript interface otherwise. A page left
 * unsolved in an outdated WebView tells the user to update it.
 */
class AndroidChallengePage(
    private val context: Context,
    private val messages: WebViewInterceptorMessages,
) : ChallengePage {
    private val executor = ContextCompat.getMainExecutor(context)

    override val isOffered: Boolean
        get() = WebViewUtil.supportsWebView(context)

    override fun open(
        url: HttpUrl,
        headers: Map<String, String>,
        userAgent: String,
        events: ChallengePageEvents,
    ): ChallengePage.Session? {
        if (Looper.myLooper() == Looper.getMainLooper()) return null
        val page = Page(url, userAgent, events)
        executor.execute { page.load(headers) }
        return ChallengePage.Session { outcome -> executor.execute { page.destroy(outcome) } }
    }

    // Read and written on the main thread
    private inner class Page(
        private val url: HttpUrl,
        private val userAgent: String,
        private val events: ChallengePageEvents,
    ) {
        private val challengeUrl = url.toString()
        private val useIsolatedWorld = WebViewFeature.isFeatureSupported(WebViewFeature.JS_INJECTION_IN_FRAME_AND_WORLD)
        private var webview: WebView? = null
        private var listenerScriptHandler: ScriptHandler? = null
        private var isRenderProcessGone = false

        @SuppressLint("SetJavaScriptEnabled")
        fun load(headers: Map<String, String>) {
            val view = WebView(context).apply {
                setDefaultSettings()
                setUserAgent(userAgent)
            }
            webview = view

            if (useIsolatedWorld) {
                // An isolated world keeps the bridge out of the page's reach
                val world = WebViewCompat.getExecutionWorld(view, JS_BRIDGE_NAME)
                val origins = setOf(url.origin())
                val listener = WebViewCompat.WebMessageListener { _, message, _, isMainFrame, _ ->
                    if (isMainFrame) message.data?.let(events::onChallengeEvent)
                }
                WebViewCompat.addWebMessageListener(view, JS_BRIDGE_NAME, origins, world, listener)
                listenerScriptHandler = WebViewCompat.addJavaScriptOnEvent(
                    view,
                    CHALLENGE_LISTENER_SCRIPT,
                    WebViewCompat.INJECTION_EVENT_DOCUMENT_START,
                    origins,
                    world,
                )
            } else {
                view.addJavascriptInterface(
                    object {
                        @Suppress("unused")
                        @JavascriptInterface
                        fun postMessage(event: String) = events.onChallengeEvent(event)
                    },
                    JS_BRIDGE_NAME,
                )
            }

            view.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    events.onPageFinished(url)

                    if (url == challengeUrl && !useIsolatedWorld) {
                        view.evaluateJavascript(CHALLENGE_LISTENER_SCRIPT, null)
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?,
                ) {
                    if (request?.isForMainFrame == true) {
                        events.onHttpError(
                            statusCode = errorResponse?.statusCode ?: 0,
                            headers = errorResponse?.responseHeaders.orEmpty(),
                        )
                    }
                }

                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    // Not handling it kills the app, failing the bypass is enough
                    logcat(LogPriority.ERROR) {
                        "WebView renderer gone during Cloudflare bypass: crashed=${detail.didCrash()}, " +
                            "priority=${detail.rendererPriorityAtExit()}, url=$challengeUrl"
                    }
                    isRenderProcessGone = true
                    events.onPageGone()
                    return true
                }
            }

            view.loadUrl(challengeUrl, headers)
        }

        fun destroy(outcome: ChallengeOutcome) {
            // Only destroy can be called on a WebView whose renderer is gone
            if (outcome == ChallengeOutcome.UNSOLVED && !isRenderProcessGone && webview?.isOutdated() == true) {
                context.toast(messages.webViewOutdated(), Toast.LENGTH_LONG)
            }

            webview?.run {
                if (!isRenderProcessGone) {
                    if (useIsolatedWorld) {
                        WebViewCompat.removeWebMessageListener(
                            this,
                            WebViewCompat.getExecutionWorld(this, JS_BRIDGE_NAME),
                            JS_BRIDGE_NAME,
                        )
                    } else {
                        removeJavascriptInterface(JS_BRIDGE_NAME)
                    }
                    listenerScriptHandler?.remove()
                    stopLoading()
                }
                destroy()
            }
        }
    }
}

private const val JS_BRIDGE_NAME = "mihon"
private val CHALLENGE_LISTENER_SCRIPT = """
    addEventListener("message", ({data}) => {
        if (data?.source === "cloudflare-challenge") {
            mihon?.postMessage(data.event);
        }
    })
""".trimIndent()

// An origin rule without a port only matches the scheme's default one
private fun HttpUrl.origin(): String {
    return if (port == HttpUrl.defaultPort(scheme)) "$scheme://$host" else "$scheme://$host:$port"
}
