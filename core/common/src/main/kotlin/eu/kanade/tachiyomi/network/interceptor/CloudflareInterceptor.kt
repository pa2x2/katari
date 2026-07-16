package eu.kanade.tachiyomi.network.interceptor

import android.annotation.SuppressLint
import android.content.Context
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

    override fun shouldIntercept(response: Response): Boolean {
        // Check if Cloudflare anti-bot is on
        return if (response.code in ERROR_CODES && response.header("Server") in SERVER_CHECK) {
            val document = Jsoup.parse(
                response.peekBody(Long.MAX_VALUE).string(),
                response.request.url.toString(),
            )

            // solve with webview only on captcha, not on geo block
            document.getElementById("challenge-error-title") != null ||
                document.getElementById("challenge-error-text") != null
        } else {
            false
        }
    }

    override fun intercept(
        chain: Interceptor.Chain,
        request: Request,
        response: Response,
    ): Response {
        val challengedClearance = response.request.cloudflareClearanceValue()
        response.close()

        return try {
            challengeCoordinator.withLock(request.url) {
                val currentClearance = cookieManager.get(request.url)
                    .firstOrNull { it.name == "cf_clearance" }

                // Another request may have solved the same Cloudflare zone while this one waited.
                // Retry with that clearance instead of deleting it and opening another WebView.
                if (shouldReuseCloudflareClearance(challengedClearance, currentClearance)) {
                    return@withLock chain.proceed(request)
                }

                cookieManager.remove(request.url, COOKIE_NAMES, 0)
                resolveWithWebView(request, currentClearance)

                chain.proceed(request)
            }
        }
        // Because OkHttp's enqueue only handles IOExceptions, wrap the exception so that
        // we don't crash the entire app
        catch (e: CloudflareBypassException) {
            throw IOException(context.stringResource(MR.strings.information_cloudflare_bypass_failure), e)
        } catch (e: Exception) {
            throw IOException(e)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun resolveWithWebView(originalRequest: Request, oldCookie: Cookie?) {
        // We need to lock this thread until the WebView finds the challenge solution url, because
        // OkHttp doesn't support asynchronous interceptors.
        val latch = CountDownLatch(1)

        var webview: WebView? = null

        var challengeFound = false
        var cloudflareBypassed = false
        var isWebViewOutdated = false

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

                    if (url == origRequestUrl && !challengeFound) {
                        // The first request didn't return the challenge, abort.
                        latch.countDown()
                    }
                }

                override fun onReceivedHttpError(
                    view: WebView?,
                    request: WebResourceRequest?,
                    errorResponse: WebResourceResponse?,
                ) {
                    if (request?.isForMainFrame == true) {
                        if (errorResponse?.statusCode in ERROR_CODES) {
                            // Found the Cloudflare challenge page.
                            challengeFound = true
                        } else {
                            // Unlock thread, the challenge wasn't found.
                            latch.countDown()
                        }
                    }
                }
            }

            webview.loadUrl(origRequestUrl, headers)
        }

        latch.awaitFor30Seconds()

        executor.execute {
            if (!cloudflareBypassed) {
                isWebViewOutdated = webview?.isOutdated() == true
            }

            webview?.run {
                stopLoading()
                destroy()
            }
        }

        // Throw exception if we failed to bypass Cloudflare
        if (!cloudflareBypassed) {
            // Prompt user to update WebView if it seems too outdated
            if (isWebViewOutdated) {
                context.toast(MR.strings.information_webview_outdated, Toast.LENGTH_LONG)
            }

            throw CloudflareBypassException()
        }
    }
}

private val ERROR_CODES = listOf(403, 503)
private val SERVER_CHECK = arrayOf("cloudflare-nginx", "cloudflare")
private val COOKIE_NAMES = listOf("cf_clearance")

private class CloudflareBypassException : Exception()

internal class CloudflareChallengeCoordinator(
    private val zoneFor: (HttpUrl) -> String = { it.topPrivateDomain() ?: it.host },
) {
    private val locks = ConcurrentHashMap<String, LockEntry>()

    fun <T> withLock(url: HttpUrl, block: () -> T): T {
        val zone = zoneFor(url)
        val entry = locks.compute(zone) { _, current ->
            (current ?: LockEntry()).also { it.users++ }
        }!!

        return try {
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

internal fun Request.cloudflareClearanceValue(): String? = headers.values("Cookie")
    .asSequence()
    .flatMap { it.splitToSequence(';') }
    .map(String::trim)
    .firstOrNull { it.substringBefore('=').trim() == "cf_clearance" }
    ?.substringAfter('=', "")
    ?.trim()
    ?.ifBlank { null }

internal fun shouldReuseCloudflareClearance(
    challengedClearance: String?,
    currentClearance: Cookie?,
): Boolean = currentClearance != null && currentClearance.value != challengedClearance
