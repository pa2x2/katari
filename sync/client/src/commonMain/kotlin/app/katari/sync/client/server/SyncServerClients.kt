package app.katari.sync.client.server

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.CookieJar
import okhttp3.EventListener
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import java.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Clients for sync servers, on the app's own HTTP client so that its DNS settings, the platform's trusted
 * certificates and Android's local network guard apply to sync too. What the client does for sources is left out:
 * their interceptors (user agent, Cloudflare challenges, logging that would print the token), the cookie jar and the
 * cache, none of which a sync server needs. Redirects aren't followed, since OkHttp would repeat a POST as a GET; a
 * server that redirects is reported as [SyncFailure.NotASyncServer].
 *
 * Most calls carry a few records or less and get short time limits, so a server that takes the connection but never
 * answers is known to be unreachable within seconds. Only the calls that read a page of records, which a small server
 * may take a while to gather and compress for a large account, wait longer; files have no limit on the whole call.
 */
@SingleIn(AppScope::class)
class SyncServerClients internal constructor(base: () -> OkHttpClient) {
    @Inject
    constructor(network: NetworkHelper) : this({ network.client.withoutSourceHandling() })

    private val transports: SyncTransports by lazy {
        val calls = base().newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(CONNECT_TIMEOUT.toJavaDuration())
            .readTimeout(READ_TIMEOUT.toJavaDuration())
            .callTimeout(CALL_TIMEOUT.toJavaDuration())
            .pingInterval(PING_INTERVAL.toJavaDuration())
            .build()
        val pages = calls.newBuilder()
            .readTimeout(PAGE_READ_TIMEOUT.toJavaDuration())
            .callTimeout(PAGE_CALL_TIMEOUT.toJavaDuration())
            .build()
        SyncTransports(calls = calls, pages = pages, files = pages.newBuilder().callTimeout(Duration.ZERO).build())
    }

    fun of(server: HttpUrl, token: String? = null): SyncServerClient = SyncServerClient(transports, server, token)

    private companion object {
        fun OkHttpClient.withoutSourceHandling(): OkHttpClient = newBuilder()
            .apply {
                interceptors().clear()
                networkInterceptors().clear()
            }
            .eventListener(EventListener.NONE)
            .cookieJar(CookieJar.NO_COOKIES)
            .cache(null)
            .build()

        val CONNECT_TIMEOUT = 10.seconds
        val READ_TIMEOUT = 15.seconds
        val CALL_TIMEOUT = 30.seconds

        // A page of a large account can take a while for a small server to read and compress.
        val PAGE_READ_TIMEOUT = 60.seconds
        val PAGE_CALL_TIMEOUT = 2.seconds * 60

        // Under the idle timeouts of common reverse proxies, as the server's own pings are.
        val PING_INTERVAL = 25.seconds
    }
}

/** The HTTP clients of [SyncServerClient], one per kind of call, sharing their connections. */
internal class SyncTransports(val calls: OkHttpClient, val pages: OkHttpClient, val files: OkHttpClient)
