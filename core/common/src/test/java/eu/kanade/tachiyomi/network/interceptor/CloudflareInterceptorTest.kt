package eu.kanade.tachiyomi.network.interceptor

import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.Headers.Companion.headersOf
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class CloudflareInterceptorTest {

    @Test
    fun `cf mitigated header detects challenge regardless of status and server`() {
        val response = response(
            code = 200,
            headers = mapOf("cf-mitigated" to "Challenge"),
        )

        assertTrue(response.isCloudflareChallenge())
    }

    @Test
    fun `legacy challenge detection accepts server value case insensitively`() {
        val response = response(
            code = 503,
            headers = mapOf("Server" to "Cloudflare"),
            body = "<html><div id=\"challenge-error-title\"></div></html>",
        )

        assertTrue(response.isCloudflareChallenge())
    }

    @Test
    fun `legacy challenge detection does not scan beyond bounded body`() {
        val response = response(
            code = 403,
            headers = mapOf("Server" to "cloudflare"),
            body = " ".repeat(64 * 1024) + "<div id=\"challenge-error-title\"></div>",
        )

        assertFalse(response.isCloudflareChallenge())
    }

    @Test
    fun `cloudflare server error without challenge marker is not intercepted`() {
        val response = response(
            code = 403,
            headers = mapOf("Server" to "cloudflare"),
            body = "<html><h1>Access denied</h1></html>",
        )

        assertFalse(response.isCloudflareChallenge())
    }

    @Test
    fun `clearance parser finds cookie across cookie headers`() {
        val request = Request.Builder()
            .url("https://example.com")
            .addHeader("Cookie", "session=abc")
            .addHeader("Cookie", "theme=dark; cf_clearance=clearance-value")
            .build()

        assertEquals("clearance-value", request.cloudflareClearanceValue())
    }

    @Test
    fun `clearance parser ignores missing and blank values`() {
        val missing = Request.Builder()
            .url("https://example.com")
            .header("Cookie", "session=abc")
            .build()
        val blank = Request.Builder()
            .url("https://example.com")
            .header("Cookie", "cf_clearance= ")
            .build()

        assertNull(missing.cloudflareClearanceValue())
        assertNull(blank.cloudflareClearanceValue())
    }

    @Test
    fun `clearance is reusable only when its value changed`() {
        val challengeUrl = "https://example.com".toHttpUrl()
        val current = Cookie.Builder()
            .name("cf_clearance")
            .value("new")
            .hostOnlyDomain("example.com")
            .build()
        val sentRequest = CloudflareSentRequest(challengeUrl, clearance = "old")

        assertTrue(shouldReuseCloudflareClearance(sentRequest, challengeUrl, current))
        assertFalse(
            shouldReuseCloudflareClearance(
                sentRequest.copy(clearance = "new"),
                challengeUrl,
                current,
            ),
        )
        assertFalse(shouldReuseCloudflareClearance(sentRequest, "https://other.example.com".toHttpUrl(), current))
        assertFalse(shouldReuseCloudflareClearance(sentRequest, challengeUrl, null))
        assertFalse(shouldReuseCloudflareClearance(null, challengeUrl, current))
    }

    @Test
    fun `network snapshot retains final redirected request and CookieJar clearance`() {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse(
                    code = 302,
                    headers = headersOf("Location", "/challenge"),
                ),
            )
            server.enqueue(MockResponse(code = 403))
            server.start()

            val clearance = Cookie.Builder()
                .name("cf_clearance")
                .value("sent-clearance")
                .hostOnlyDomain(server.hostName)
                .build()
            val client = OkHttpClient.Builder()
                .cookieJar(fixedCookieJar(clearance))
                .addInterceptor { chain ->
                    chain.proceed(chain.request().withCloudflareRequestSnapshot())
                }
                .addNetworkInterceptor(CloudflareRequestSnapshotInterceptor())
                .build()

            client.newCall(Request.Builder().url(server.url("/")).build()).execute().use { response ->
                assertNull(response.request.header("Cookie"))
                assertEquals(clearance.value, response.request.cloudflareSentRequest()?.clearance)
                assertEquals(response.request.url, response.request.cloudflareSentRequest()?.url)
                assertEquals("/challenge", response.request.url.encodedPath)
            }
        }
    }

    @Test
    fun `same zone callers share one solve`() {
        val executor = Executors.newFixedThreadPool(2)
        val solveStarted = CountDownLatch(1)
        val releaseSolve = CountDownLatch(1)
        val secondRegistered = CountDownLatch(1)
        val solveCount = AtomicInteger()
        val coordinator = CloudflareChallengeCoordinator(
            zoneFor = { "example.com" },
            onRegistered = { url ->
                if (url.host == "b.example.com") secondRegistered.countDown()
            },
        )

        try {
            val first = executor.submit {
                coordinator.solve("https://a.example.com".toHttpUrl()) {
                    solveCount.incrementAndGet()
                    solveStarted.countDown()
                    assertTrue(releaseSolve.await(5, TimeUnit.SECONDS))
                }
            }
            assertTrue(solveStarted.await(5, TimeUnit.SECONDS))

            val second = executor.submit {
                coordinator.solve("https://b.example.com".toHttpUrl()) {
                    solveCount.incrementAndGet()
                }
            }
            assertTrue(secondRegistered.await(5, TimeUnit.SECONDS))
            releaseSolve.countDown()

            first.get(5, TimeUnit.SECONDS)
            second.get(5, TimeUnit.SECONDS)
            assertEquals(1, solveCount.get())
        } finally {
            releaseSolve.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `same zone callers share solve failure and a later call can retry`() {
        val executor = Executors.newFixedThreadPool(2)
        val solveStarted = CountDownLatch(1)
        val releaseSolve = CountDownLatch(1)
        val secondRegistered = CountDownLatch(1)
        val failure = IllegalStateException("solve failed")
        val solveCount = AtomicInteger()
        val coordinator = CloudflareChallengeCoordinator(
            zoneFor = { "example.com" },
            onRegistered = { url ->
                if (url.host == "b.example.com") secondRegistered.countDown()
            },
        )

        try {
            val first = executor.submit {
                coordinator.solve("https://a.example.com".toHttpUrl()) {
                    solveCount.incrementAndGet()
                    solveStarted.countDown()
                    assertTrue(releaseSolve.await(5, TimeUnit.SECONDS))
                    throw failure
                }
            }
            assertTrue(solveStarted.await(5, TimeUnit.SECONDS))

            val second = executor.submit {
                coordinator.solve("https://b.example.com".toHttpUrl()) {
                    solveCount.incrementAndGet()
                }
            }
            assertTrue(secondRegistered.await(5, TimeUnit.SECONDS))
            releaseSolve.countDown()

            val firstFailure = assertThrows(ExecutionException::class.java) {
                first.get(5, TimeUnit.SECONDS)
            }
            val secondFailure = assertThrows(ExecutionException::class.java) {
                second.get(5, TimeUnit.SECONDS)
            }
            assertEquals(failure, firstFailure.cause)
            assertEquals(failure, secondFailure.cause)
            assertEquals(1, solveCount.get())

            coordinator.solve("https://c.example.com".toHttpUrl()) {
                solveCount.incrementAndGet()
            }
            assertEquals(2, solveCount.get())
        } finally {
            releaseSolve.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `different zones solve independently`() {
        val coordinator = CloudflareChallengeCoordinator(zoneFor = { it.host })
        val executor = Executors.newFixedThreadPool(2)
        val firstStarted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondFinished = CountDownLatch(1)

        try {
            val first = executor.submit {
                coordinator.solve("https://a.example.com".toHttpUrl()) {
                    firstStarted.countDown()
                    assertTrue(releaseFirst.await(5, TimeUnit.SECONDS))
                }
            }
            assertTrue(firstStarted.await(5, TimeUnit.SECONDS))

            val second = executor.submit {
                coordinator.solve("https://b.example.com".toHttpUrl()) {
                    secondFinished.countDown()
                }
            }

            assertTrue(secondFinished.await(5, TimeUnit.SECONDS))
            second.get(5, TimeUnit.SECONDS)
            releaseFirst.countDown()
            first.get(5, TimeUnit.SECONDS)
        } finally {
            releaseFirst.countDown()
            executor.shutdownNow()
        }
    }

    @Test
    fun `interrupted solve lets a registered waiter take over`() {
        val executor = Executors.newFixedThreadPool(2)
        val firstStarted = CountDownLatch(1)
        val releaseFirst = CountDownLatch(1)
        val secondRegistered = CountDownLatch(1)
        val solveCount = AtomicInteger()
        val coordinator = CloudflareChallengeCoordinator(
            zoneFor = { "example.com" },
            onRegistered = { url ->
                if (url.host == "b.example.com") secondRegistered.countDown()
            },
        )

        try {
            val first = executor.submit {
                coordinator.solve("https://a.example.com".toHttpUrl()) {
                    solveCount.incrementAndGet()
                    firstStarted.countDown()
                    assertTrue(releaseFirst.await(5, TimeUnit.SECONDS))
                    throw InterruptedException("solve interrupted")
                }
            }
            assertTrue(firstStarted.await(5, TimeUnit.SECONDS))

            val second = executor.submit {
                coordinator.solve("https://b.example.com".toHttpUrl()) {
                    assertFalse(Thread.currentThread().isInterrupted)
                    solveCount.incrementAndGet()
                }
            }

            assertTrue(secondRegistered.await(5, TimeUnit.SECONDS))
            releaseFirst.countDown()

            val firstFailure = assertThrows(ExecutionException::class.java) {
                first.get(5, TimeUnit.SECONDS)
            }
            assertTrue(firstFailure.cause is InterruptedException)
            second.get(5, TimeUnit.SECONDS)
            assertEquals(2, solveCount.get())
        } finally {
            releaseFirst.countDown()
            executor.shutdownNow()
        }
    }

    private fun response(
        code: Int,
        headers: Map<String, String> = emptyMap(),
        body: String = "",
    ): Response {
        val request = Request.Builder()
            .url("https://example.com")
            .build()
        val builder = Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message("test")
            .body(body.toResponseBody())
        headers.forEach { (name, value) -> builder.header(name, value) }
        return builder.build()
    }

    private fun fixedCookieJar(cookie: Cookie): CookieJar = object : CookieJar {
        override fun loadForRequest(url: okhttp3.HttpUrl): List<Cookie> = listOf(cookie)

        override fun saveFromResponse(url: okhttp3.HttpUrl, cookies: List<Cookie>) = Unit
    }
}
