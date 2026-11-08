package eu.kanade.tachiyomi.network.interceptor

import app.katari.core.common.web.ChallengeOutcome
import app.katari.core.common.web.ChallengePage
import app.katari.core.common.web.ChallengePageEvents
import app.katari.core.common.web.ChallengeUnsolved
import app.katari.core.common.web.ChallengeUnsolvedException
import app.katari.core.common.web.webViewSafeHeaders
import eu.kanade.tachiyomi.network.AndroidCookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.time.Duration.Companion.seconds

/**
 * Gets requests through Cloudflare's challenge for both platforms' [CloudflareInterceptor]: the challenged request
 * waits while a [page] nobody sees runs the challenge and leaves its clearance in [cookieJar], then goes again with it.
 * One request at a time solves a zone's challenge; the others find its clearance and try that first.
 *
 * OkHttp's interceptors can't wait asynchronously, so the request's thread blocks for up to [CHALLENGE_TIME_LIMIT].
 * A challenge that needs a person, fails or isn't found ends the wait early, and the request fails with
 * [WebViewInterceptorMessages.cloudflareBypassFailed], which screens follow with the offer to open the site in the
 * web view.
 */
internal class CloudflareChallengeSolver(
    private val cookieJar: AndroidCookieJar,
    private val page: ChallengePage,
    private val defaultUserAgentProvider: () -> String,
    private val messages: WebViewInterceptorMessages,
) {
    private val coordinator = CloudflareChallengeCoordinator()

    /** Solves the challenge [response] answered [request] with and sends [request] again; throws if it can't. */
    fun solve(chain: Interceptor.Chain, request: Request, response: Response): Response {
        var challengeRequest = response.request
        var sentRequest = challengeRequest.cloudflareSentRequest()
        response.close()

        return try {
            repeat(MAX_CHALLENGE_SOLVE_ATTEMPTS) {
                coordinator.solve(challengeRequest.url) {
                    val clearanceBeforeSolve = cookieJar.cloudflareClearance(challengeRequest.url)

                    // Another request may have solved this challenge generation before this
                    // request entered the coordinator. Validate that clearance before opening
                    // another page.
                    if (!shouldReuseCloudflareClearance(sentRequest, challengeRequest.url, clearanceBeforeSolve)) {
                        cookieJar.remove(challengeRequest.url, COOKIE_NAMES, 0)
                        solveInPage(challengeRequest, request, clearanceBeforeSolve)
                    }
                }

                val retryResponse = chain.proceed(request.withCloudflareRequestSnapshot())
                if (!retryResponse.isCloudflareChallenge()) {
                    return retryResponse
                }

                challengeRequest = retryResponse.request
                sentRequest = challengeRequest.cloudflareSentRequest()
                retryResponse.close()
            }

            throw ChallengeUnsolvedException(ChallengeUnsolved.REJECTED)
        }
        // Because OkHttp's enqueue only handles IOExceptions, wrap the exception so that
        // we don't crash the entire app
        catch (e: ChallengeUnsolvedException) {
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

    /**
     * Solves the challenge [originalRequest] got, as [retried] is sent: the clearance holds only for the user agent
     * that earned it, and an extension's own interceptor may have sent the challenged request with another one.
     */
    private fun solveInPage(originalRequest: Request, retried: Request, oldCookie: Cookie?) {
        // A page can only load with GET, and loading the url of another method usually reaches an endpoint that
        // answers GET without a challenge to solve. The clearance covers the whole site, so it's earned on the site's
        // root page instead.
        val challengeUrl = if (originalRequest.method == "GET") {
            originalRequest.url
        } else {
            originalRequest.url.newBuilder().encodedPath("/").query(null).fragment(null).build()
        }
        val wait = PageWait(originalRequest.url, oldCookie)
        val session = page.open(
            url = challengeUrl,
            headers = retried.headers.webViewSafeHeaders(),
            userAgent = retried.webViewUserAgent(),
            events = wait,
        ) ?: throw ChallengeUnsolvedException(ChallengeUnsolved.NO_PAGE)

        var outcome = ChallengeOutcome.INTERRUPTED
        try {
            wait.await()
            outcome = if (wait.bypassed) ChallengeOutcome.SOLVED else ChallengeOutcome.UNSOLVED
        } finally {
            session.close(outcome)
        }

        if (!wait.bypassed) {
            // A clearance from a failed challenge would only be rejected again, and requests waiting on this
            // challenge would take it for a fresh one
            cookieJar.remove(originalRequest.url, COOKIE_NAMES, 0)
            throw ChallengeUnsolvedException(wait.unsolved)
        }
    }

    // Avoid sending empty User-Agent, Chromium will reset to default if empty
    private fun Request.webViewUserAgent(): String =
        header("User-Agent")?.takeIf { it.isNotEmpty() } ?: defaultUserAgentProvider()

    /** The wait for one page: it ends once the page got a clearance other than [oldCookie] or can't get one. */
    private inner class PageWait(private val url: HttpUrl, private val oldCookie: Cookie?) : ChallengePageEvents {
        private val latch = CountDownLatch(1)

        @Volatile
        var bypassed = false
            private set

        // The first reason the page gave up for; until then, the time running out
        @Volatile
        var unsolved = ChallengeUnsolved.TIMED_OUT
            private set

        @Volatile
        private var challengeFailed = false

        // Cloudflare can answer the page's solution with another challenge and set the clearance with it, before the
        // clearance is good: the load that challenge ends is no solve, the load after it may be
        private val loadChallenged = AtomicBoolean(false)

        fun await() {
            latch.await(CHALLENGE_TIME_LIMIT.inWholeSeconds, TimeUnit.SECONDS)
        }

        override fun onPageFinished(url: String) {
            if (loadChallenged.getAndSet(false)) return
            val clearance = cookieJar.cloudflareClearance(this.url)
            if (!challengeFailed && clearance != null && clearance != oldCookie) {
                bypassed = true
                latch.countDown()
            }
        }

        override fun onHttpError(statusCode: Int, headers: Map<String, String>) {
            if (isCloudflareChallengeResponse(headers)) {
                loadChallenged.set(true)
            } else if (!isCloudflareChallengeError(statusCode, headers)) {
                giveUp(ChallengeUnsolved.NOT_FOUND)
            }
        }

        override fun onChallengeEvent(event: String) {
            when (event) {
                // The challenge cannot be solved non-interactively, abort.
                "interactiveBegin" -> giveUp(ChallengeUnsolved.NEEDS_PERSON)
                "fail" -> {
                    challengeFailed = true
                    giveUp(ChallengeUnsolved.FAILED)
                }
            }
        }

        override fun onPageGone() {
            giveUp(ChallengeUnsolved.PAGE_GONE)
        }

        private fun giveUp(reason: ChallengeUnsolved) {
            synchronized(this) {
                if (latch.count == 0L) return
                unsolved = reason
                latch.countDown()
            }
        }
    }
}

/** How long a request waits for a page to solve its challenge. */
private val CHALLENGE_TIME_LIMIT = 30.seconds
private const val MAX_CHALLENGE_SOLVE_ATTEMPTS = 2
private val COOKIE_NAMES = listOf("cf_clearance")

private fun AndroidCookieJar.cloudflareClearance(url: HttpUrl): Cookie? {
    return get(url).firstOrNull { it.name == "cf_clearance" }
}
