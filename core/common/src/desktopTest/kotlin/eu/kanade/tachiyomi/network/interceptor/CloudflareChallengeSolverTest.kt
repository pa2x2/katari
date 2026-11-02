package eu.kanade.tachiyomi.network.interceptor

import eu.kanade.tachiyomi.network.AndroidCookieJar
import mihon.core.common.network.cookie.AppCookieStore
import mihon.core.common.web.ChallengePage
import mihon.core.common.web.ChallengePageEvents
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.Cookie
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

class CloudflareChallengeSolverTest {

    @Test
    fun `a clearance another request got is reused only by requests that didn't send it`() {
        val site = ChallengedSite()
        MockWebServer().use { server ->
            server.dispatcher = site
            server.start()
            val cookieJar = AndroidCookieJar()
            val page = ClearancePage(server.hostName, cookieJar, site, firstSolveAwaits = 2)
            val solver = CloudflareChallengeSolver(cookieJar, page, { "Test" }, Messages)
            val client = OkHttpClient.Builder()
                .cookieJar(cookieJar)
                .addInterceptor { chain ->
                    val request = chain.request().withCloudflareRequestSnapshot()
                    val response = chain.proceed(request)
                    if (response.isCloudflareChallenge()) solver.solve(chain, request, response) else response
                }
                .addNetworkInterceptor(CloudflareRequestSnapshotInterceptor())
                .build()
            fun get(path: String) = client.newCall(Request.Builder().url(server.url(path)).build())
                .execute()
                .use { it.code }

            // Both are challenged before the page solves; the second finds the first one's clearance
            val requests = Executors.newFixedThreadPool(2)
            try {
                val first = requests.submit<Int> { get("/a") }
                val second = requests.submit<Int> { get("/b") }
                assertEquals(200, first.get(10, TimeUnit.SECONDS))
                assertEquals(200, second.get(10, TimeUnit.SECONDS))
            } finally {
                requests.shutdownNow()
            }
            assertEquals(1, page.opened.get())

            // The site stops taking that clearance, which the next request then sends and must not reuse
            site.validClearance = null
            assertEquals(200, get("/c"))
            assertEquals(2, page.opened.get())
        }
    }

    /** A site behind a challenge that lets through only requests with [validClearance]. */
    private class ChallengedSite : Dispatcher() {
        @Volatile
        var validClearance: String? = null
        val challenged = AtomicInteger()

        override fun dispatch(request: RecordedRequest): MockResponse {
            val clearance = request.headers.values("Cookie")
                .flatMap { it.split(';') }
                .map(String::trim)
                .firstOrNull { it.startsWith("cf_clearance=") }
                ?.substringAfter('=')
            if (clearance != null && clearance == validClearance) return MockResponse(code = 200)
            challenged.incrementAndGet()
            return MockResponse(code = 403, headers = headersOf("cf-mitigated", "challenge"))
        }
    }

    /**
     * Solves on a thread of its own, as a platform's page does: it leaves a new clearance in [cookieJar] before it
     * reports the finished load. The first solve waits until [site] has challenged [firstSolveAwaits] requests.
     */
    private class ClearancePage(
        private val host: String,
        private val cookieJar: AndroidCookieJar,
        private val site: ChallengedSite,
        private val firstSolveAwaits: Int,
    ) : ChallengePage {
        val opened = AtomicInteger()

        override val isOffered = true

        override fun open(
            url: HttpUrl,
            headers: Map<String, String>,
            userAgent: String,
            events: ChallengePageEvents,
        ): ChallengePage.Session {
            val generation = opened.incrementAndGet()
            val closed = CountDownLatch(1)
            thread {
                val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(5)
                while (generation == 1 && site.challenged.get() < firstSolveAwaits && System.nanoTime() < deadline) {
                    if (closed.await(10, TimeUnit.MILLISECONDS)) return@thread
                }
                val clearance = "generation-$generation"
                site.validClearance = clearance
                cookieJar.saveFromResponse(
                    url,
                    listOf(Cookie.Builder().name("cf_clearance").value(clearance).hostOnlyDomain(host).build()),
                )
                events.onPageFinished(url.toString())
            }
            return ChallengePage.Session { closed.countDown() }
        }
    }

    private object Messages : WebViewInterceptorMessages {
        override fun webViewRequired() = "web view required"

        override fun webViewOutdated() = "web view outdated"

        override fun cloudflareBypassFailed() = "bypass failed"
    }

    companion object {
        @JvmStatic
        @BeforeAll
        fun openCookieStore(@TempDir directory: Path) {
            AppCookieStore.open(directory)
        }
    }
}
