package eu.kanade.tachiyomi.network.interceptor

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
import eu.kanade.tachiyomi.network.AndroidCookieJar
import eu.kanade.tachiyomi.util.system.isOutdated
import eu.kanade.tachiyomi.util.system.toWebViewHeaders
import eu.kanade.tachiyomi.util.system.toast
import logcat.LogPriority
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import tachiyomi.core.common.util.system.logcat
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

class CloudflareInterceptor(
    private val context: Context,
    private val cookieManager: AndroidCookieJar,
    defaultUserAgentProvider: () -> String,
    messages: WebViewInterceptorMessages,
) : WebViewInterceptor(context, defaultUserAgentProvider, messages) {

    private val executor = ContextCompat.getMainExecutor(context)
    private val challengeCoordinator = CloudflareChallengeCoordinator()

    override fun prepareRequest(request: Request): Request {
        return request.withCloudflareRequestSnapshot()
    }

    override fun shouldIntercept(response: Response): Boolean {
        return response.isCloudflareChallenge()
    }

    override fun intercept(
        chain: Interceptor.Chain,
        request: Request,
        response: Response,
    ): Response {
        var challengeRequest = response.request
        var sentRequest = challengeRequest.cloudflareSentRequest()
        response.close()

        return try {
            repeat(MAX_CHALLENGE_SOLVE_ATTEMPTS) {
                challengeCoordinator.solve(challengeRequest.url) {
                    val clearanceBeforeSolve = cookieManager.cloudflareClearance(challengeRequest.url)

                    // Another request may have solved this challenge generation before this
                    // request entered the coordinator. Validate that clearance before opening
                    // another WebView.
                    if (!shouldReuseCloudflareClearance(sentRequest, challengeRequest.url, clearanceBeforeSolve)) {
                        cookieManager.remove(challengeRequest.url, COOKIE_NAMES, 0)
                        resolveWithWebView(challengeRequest, clearanceBeforeSolve)
                    }
                }

                val retryResponse = chain.proceed(request.withCloudflareRequestSnapshot())
                if (!shouldIntercept(retryResponse)) {
                    return retryResponse
                }

                challengeRequest = retryResponse.request
                sentRequest = challengeRequest.cloudflareSentRequest()
                retryResponse.close()
            }

            throw CloudflareBypassException()
        }
        // Because OkHttp's enqueue only handles IOExceptions, wrap the exception so that
        // we don't crash the entire app
        catch (e: CloudflareBypassException) {
            throw IOException(messages.cloudflareBypassFailed(), e)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException(e)
        } catch (e: IOException) {
            throw e
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(originalRequest: Request, oldCookie: Cookie?) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            throw CloudflareBypassException()
        }

        // We need to lock this thread until the WebView finds the challenge solution url, because
        // OkHttp doesn't support asynchronous interceptors.
        val latch = CountDownLatch(1)

        var webview: WebView? = null
        var listenerScriptHandler: ScriptHandler? = null
        val useIsolatedWorld = WebViewFeature.isFeatureSupported(WebViewFeature.JS_INJECTION_IN_FRAME_AND_WORLD)

        var cloudflareBypassed = false
        var challengeFailed = false
        var isRenderProcessGone = false

        // A WebView can only load a page with GET, and loading the url of another method usually reaches an
        // endpoint that answers GET without a challenge to solve. The clearance covers the whole site, so it's
        // earned on the site's root page instead.
        val challengeUrl = if (originalRequest.method == "GET") {
            originalRequest.url.toString()
        } else {
            originalRequest.url.newBuilder().encodedPath("/").query(null).fragment(null).build().toString()
        }
        val headers = originalRequest.headers.toWebViewHeaders()

        executor.execute {
            val view = createWebView(originalRequest)
            webview = view

            fun onChallengeEvent(event: String) {
                when (event) {
                    // The challenge cannot be solved non-interactively, abort.
                    "interactiveBegin" -> latch.countDown()
                    "fail" -> {
                        challengeFailed = true
                        latch.countDown()
                    }
                }
            }

            if (useIsolatedWorld) {
                // An isolated world keeps the bridge out of the page's reach
                val world = WebViewCompat.getExecutionWorld(view, JS_BRIDGE_NAME)
                val origins = setOf(originalRequest.url.origin())
                val listener = WebViewCompat.WebMessageListener { _, message, _, isMainFrame, _ ->
                    if (isMainFrame) message.data?.let(::onChallengeEvent)
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
                        fun postMessage(event: String) = onChallengeEvent(event)
                    },
                    JS_BRIDGE_NAME,
                )
            }

            view.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    val clearance = cookieManager.cloudflareClearance(originalRequest.url)
                    if (!challengeFailed && clearance != null && clearance != oldCookie) {
                        cloudflareBypassed = true
                        latch.countDown()
                    }

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
                        val isChallengeResponse = errorResponse?.statusCode in ERROR_CODES ||
                            errorResponse?.responseHeaders
                                ?.entries
                                ?.any { (name, value) ->
                                    name.equals("cf-mitigated", ignoreCase = true) &&
                                        value.equals("challenge", ignoreCase = true)
                                } == true
                        if (!isChallengeResponse) {
                            // Unlock thread, the challenge wasn't found.
                            latch.countDown()
                        }
                    }
                }

                override fun onRenderProcessGone(view: WebView, detail: RenderProcessGoneDetail): Boolean {
                    // Not handling it kills the app, failing the bypass is enough
                    logcat(LogPriority.ERROR) {
                        "WebView renderer gone during Cloudflare bypass: crashed=${detail.didCrash()}, " +
                            "priority=${detail.rendererPriorityAtExit()}, url=$challengeUrl"
                    }
                    isRenderProcessGone = true
                    latch.countDown()
                    return true
                }
            }

            view.loadUrl(challengeUrl, headers)
        }

        var waitCompleted = false
        try {
            latch.awaitFor30Seconds()
            waitCompleted = true
        } finally {
            executor.execute {
                // Only destroy can be called on a WebView whose renderer is gone
                if (waitCompleted && !cloudflareBypassed && !isRenderProcessGone && webview?.isOutdated() == true) {
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

        // Throw exception if we failed to bypass Cloudflare
        if (!cloudflareBypassed) {
            // A clearance from a failed challenge would only be rejected again, and requests waiting on this
            // challenge would take it for a fresh one
            cookieManager.remove(originalRequest.url, COOKIE_NAMES, 0)
            throw CloudflareBypassException()
        }
    }
}

private val ERROR_CODES = listOf(403, 503)
private const val JS_BRIDGE_NAME = "mihon"
private val CHALLENGE_LISTENER_SCRIPT = """
    addEventListener("message", ({data}) => {
        if (data?.source === "cloudflare-challenge") {
            mihon?.postMessage(data.event);
        }
    })
""".trimIndent()
private const val MAX_LEGACY_CHALLENGE_BODY_BYTES = 64L * 1024
private const val MAX_CHALLENGE_SOLVE_ATTEMPTS = 2
private val SERVER_CHECK = setOf("cloudflare-nginx", "cloudflare")
private val COOKIE_NAMES = listOf("cf_clearance")

private class CloudflareBypassException : Exception()

internal class CloudflareRequestSnapshotInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        request.tag(CloudflareRequestState::class.java)?.sentRequest = CloudflareSentRequest(
            url = request.url,
            clearance = request.cloudflareClearanceValue(),
        )
        return chain.proceed(request)
    }
}

internal class CloudflareRequestState {
    @Volatile
    var sentRequest: CloudflareSentRequest? = null
}

internal data class CloudflareSentRequest(
    val url: HttpUrl,
    val clearance: String?,
)

internal fun Request.withCloudflareRequestSnapshot(): Request = newBuilder()
    .tag(CloudflareRequestState::class.java, CloudflareRequestState())
    .build()

internal fun Request.cloudflareSentRequest(): CloudflareSentRequest? =
    tag(CloudflareRequestState::class.java)?.sentRequest

internal fun Response.isCloudflareChallenge(): Boolean {
    if (header("cf-mitigated").equals("challenge", ignoreCase = true)) {
        return true
    }

    val isLegacyCloudflareResponse = code in ERROR_CODES &&
        SERVER_CHECK.any { server -> header("Server").equals(server, ignoreCase = true) }
    if (!isLegacyCloudflareResponse) {
        return false
    }

    val document = Jsoup.parse(
        peekBody(MAX_LEGACY_CHALLENGE_BODY_BYTES).string(),
        request.url.toString(),
    )

    return document.getElementById("challenge-error-title") != null ||
        document.getElementById("challenge-error-text") != null
}

internal class CloudflareChallengeCoordinator(
    private val zoneFor: (HttpUrl) -> String = { it.topPrivateDomain() ?: it.host },
    private val onRegistered: (HttpUrl) -> Unit = {},
) {
    private val locks = ConcurrentHashMap<String, LockEntry>()

    fun solve(url: HttpUrl, block: () -> Unit) {
        val zone = zoneFor(url)
        val entry = locks.compute(zone) { _, current ->
            (current ?: LockEntry()).also { it.users++ }
        }!!

        try {
            onRegistered(url)
            synchronized(entry.monitor) {
                block()
            }
        } finally {
            locks.computeIfPresent(zone) { _, current ->
                check(current === entry)
                current.users--
                current.takeIf { it.users > 0 }
            }
        }
    }

    private class LockEntry(
        val monitor: Any = Any(),
        var users: Int = 0,
    )
}

// An origin rule without a port only matches the scheme's default one
private fun HttpUrl.origin(): String {
    return if (port == HttpUrl.defaultPort(scheme)) "$scheme://$host" else "$scheme://$host:$port"
}

private fun AndroidCookieJar.cloudflareClearance(url: HttpUrl): Cookie? {
    return get(url).firstOrNull { it.name == "cf_clearance" }
}

internal fun Request.cloudflareClearanceValue(): String? = headers.values("Cookie")
    .asSequence()
    .flatMap { it.splitToSequence(';') }
    .map(String::trim)
    .firstOrNull { it.substringBefore('=').trim() == "cf_clearance" }
    ?.substringAfter('=', "")
    ?.trim()
    ?.ifBlank { null }

internal fun shouldReuseCloudflareClearance(
    sentRequest: CloudflareSentRequest?,
    challengeUrl: HttpUrl,
    currentClearance: Cookie?,
): Boolean = sentRequest?.url == challengeUrl &&
    currentClearance != null &&
    currentClearance.value != sentRequest.clearance
