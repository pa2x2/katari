package eu.kanade.tachiyomi.network.interceptor

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.core.content.ContextCompat
import eu.kanade.tachiyomi.network.AndroidCookieJar
import eu.kanade.tachiyomi.util.system.isOutdated
import eu.kanade.tachiyomi.util.system.toast
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import tachiyomi.core.common.i18n.stringResource
import tachiyomi.i18n.MR
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CountDownLatch

class CloudflareInterceptor(
    private val context: Context,
    private val cookieManager: AndroidCookieJar,
    defaultUserAgentProvider: () -> String,
) : WebViewInterceptor(context, defaultUserAgentProvider) {

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
            throw IOException(context.stringResource(MR.strings.information_cloudflare_bypass_failure), e)
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

        var cloudflareBypassed = false

        val origRequestUrl = originalRequest.url.toString()
        val headers = parseHeaders(originalRequest.headers)

        executor.execute {
            webview = createWebView(originalRequest)

            webview.webViewClient = object : WebViewClient() {
                override fun onPageFinished(view: WebView, url: String) {
                    fun isCloudFlareBypassed(): Boolean {
                        return cookieManager.get(origRequestUrl.toHttpUrl())
                            .firstOrNull { it.name == "cf_clearance" }
                            .let { it != null && it != oldCookie }
                    }

                    if (isCloudFlareBypassed()) {
                        cloudflareBypassed = true
                        latch.countDown()
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
            }

            webview.loadUrl(origRequestUrl, headers)
        }

        var waitCompleted = false
        try {
            latch.awaitFor30Seconds()
            waitCompleted = true
        } finally {
            executor.execute {
                if (waitCompleted && !cloudflareBypassed && webview?.isOutdated() == true) {
                    context.toast(MR.strings.information_webview_outdated, Toast.LENGTH_LONG)
                }

                webview?.run {
                    stopLoading()
                    destroy()
                }
            }
        }

        // Throw exception if we failed to bypass Cloudflare
        if (!cloudflareBypassed) {
            throw CloudflareBypassException()
        }
    }
}

private val ERROR_CODES = listOf(403, 503)
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
