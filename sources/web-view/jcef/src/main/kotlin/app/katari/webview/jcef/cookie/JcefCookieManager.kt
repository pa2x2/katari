package app.katari.webview.jcef.cookie

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull
import logcat.LogPriority
import org.cef.network.CefCookie
import org.cef.network.CefCookieManager
import tachiyomi.core.common.util.system.logcat
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Chromium's cookie manager through JCEF, which only a runtime with JCEF can load. [manager] is held for as long as
 * Chromium runs, as JCEF stops a manager it garbage collects.
 *
 * JCEF reports neither when a change has been applied nor when a read has finished, and doesn't order the two: it
 * passes changes to Chromium through its IO thread and reads straight through its UI thread, so a read can overtake
 * a change made before it, and a read of no cookies never calls back at all. [read] therefore first sets a marker
 * cookie, a session cookie of a host that never resolves, with a new value, and reads until it sees that value: the
 * changes made before it have been applied by then, and the read has at least that one cookie, whose count tells when
 * the read is complete. Chromium doesn't restore session cookies after a restart, so the marker never outlives a run.
 */
internal class JcefCookieManager(private val manager: CefCookieManager) : ChromiumCookieManager {
    private val markerValues = AtomicLong()

    override fun set(cookie: ChromiumCookie) {
        val now = Date()
        val cefCookie = CefCookie(
            cookie.name,
            cookie.value,
            // Chromium makes a host-only cookie of an empty domain, for the URL's host
            if (cookie.domain == cookie.host) "" else cookie.domain,
            cookie.path,
            cookie.secure,
            cookie.httpOnly,
            now,
            now,
            cookie.expiresAt != null,
            Date(cookie.expiresAt ?: now.time),
        )
        val set = whileServerRuns { manager.setCookie(cookie.url, cefCookie) }
        if (set != true) {
            logcat(LogPriority.WARN) { "Chromium didn't take the cookie ${cookie.name} of ${cookie.domain}" }
        }
    }

    // Setting an expired cookie doesn't remove it: Chromium's server keeps it as a session cookie with an empty value,
    // which the mirror read back into the store and requests then sent
    override fun remove(cookie: ChromiumCookie) {
        if (whileServerRuns { manager.deleteCookies(cookie.url, cookie.name) } != true) {
            logcat(LogPriority.WARN) { "Chromium didn't take the removal of ${cookie.name} of ${cookie.domain}" }
        }
    }

    override fun removeAll() {
        if (whileServerRuns { manager.deleteCookies("", "") } != true) {
            logcat(LogPriority.WARN) {
                "Chromium didn't take the removal of its cookies"
            }
        }
    }

    override suspend fun read(): List<ChromiumCookie>? = withTimeoutOrNull(READ_TIMEOUT) {
        val marker = MARKER.copy(value = markerValues.incrementAndGet().toString())
        set(marker)
        var cookies = visitAll()
        while (cookies == null || cookies.none { it.key == marker.key && it.value == marker.value }) {
            delay(RETRY_INTERVAL)
            cookies = visitAll()
        }
        cookies.filterNot { it.key == marker.key }
    }

    /** Every cookie of one visit, or null where it found none, as one that comes before the marker may. */
    private suspend fun visitAll(): List<ChromiumCookie>? {
        val visited = ConcurrentHashMap<Int, ChromiumCookie>()
        val complete = CompletableDeferred<List<ChromiumCookie>>()
        // JCEF calls the visitor on a new thread for each cookie, so they may arrive out of order
        val visiting = whileServerRuns {
            manager.visitAllCookies { cookie, index, total, _ ->
                visited[index] = cookie.toChromiumCookie()
                if (visited.size == total) complete.complete(visited.values.toList())
                true
            }
        }
        if (visiting != true) return null
        // A visit of no cookies never ends, so one is given up once its cookies stop arriving
        var arrived = -1
        while (visited.size != arrived) {
            arrived = visited.size
            withTimeoutOrNull(VISIT_IDLE_TIMEOUT) { complete.await() }?.let { return it }
        }
        return null
    }

    /**
     * [call]'s answer, or null where Chromium's server has ended, as after a crash. JCEF fails the first call that
     * finds the server gone, then answers each call with null, which its own methods unbox. The mirror stops calling
     * once the runtime learns of it, so only calls made as the server ends get here.
     */
    private inline fun <T> whileServerRuns(call: () -> T): T? = try {
        call()
    } catch (_: IllegalStateException) {
        null
    } catch (_: NullPointerException) {
        null
    }

    private val ChromiumCookie.url: String
        get() {
            // An IPv6 host goes in brackets
            val urlHost = if (':' in host) "[$host]" else host
            // Chromium takes a Secure cookie only from an HTTPS URL, and any other cookie from one too
            return "https://$urlHost$path"
        }

    private fun CefCookie.toChromiumCookie() = ChromiumCookie(
        name = name,
        value = value,
        domain = domain,
        path = path,
        secure = secure,
        httpOnly = httponly,
        expiresAt = expires?.time?.takeIf { hasExpires },
    )

    private companion object {
        val MARKER = ChromiumCookie(
            name = "katari-cookie-read",
            value = "",
            // .invalid never resolves (RFC 2606), so no page can read or change it
            domain = "cookies.katari.invalid",
            path = "/",
            secure = true,
            httpOnly = true,
            expiresAt = null,
        )

        val READ_TIMEOUT = 5.seconds
        val VISIT_IDLE_TIMEOUT = 50.milliseconds
        val RETRY_INTERVAL = 10.milliseconds
    }
}
