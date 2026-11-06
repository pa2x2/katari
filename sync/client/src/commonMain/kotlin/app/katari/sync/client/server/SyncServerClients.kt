package app.katari.sync.client.server

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.CookieJar
import okhttp3.EventListener
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Clients for sync servers, on the app's own HTTP client so that its DNS settings, the platform's trusted
 * certificates and Android's local network guard apply to sync too. What the client does for sources is left out:
 * their interceptors (user agent, Cloudflare challenges, logging that would print the token), the cookie jar and the
 * cache, none of which a sync server needs. Redirects aren't followed, since OkHttp would repeat a POST as a GET; a
 * server that redirects is reported as [SyncFailure.NotASyncServer].
 */
@SingleIn(AppScope::class)
class SyncServerClients internal constructor(base: () -> OkHttpClient) {
    @Inject
    constructor(network: NetworkHelper) : this({ network.client.withoutSourceHandling() })

    private val http: OkHttpClient by lazy {
        base().newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(CONNECT_TIMEOUT.toJavaDuration())
            .readTimeout(READ_TIMEOUT.toJavaDuration())
            .callTimeout(CALL_TIMEOUT.toJavaDuration())
            .pingInterval(PING_INTERVAL.toJavaDuration())
            .build()
    }

    fun of(server: HttpUrl, token: String? = null): SyncServerClient = SyncServerClient(http, server, token)

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

        val CONNECT_TIMEOUT = 15.seconds

        // A page of a large account can take a while for a small server to read and compress.
        val READ_TIMEOUT = 60.seconds
        val CALL_TIMEOUT = 2.seconds * 60

        // Under the idle timeouts of common reverse proxies, as the server's own pings are.
        val PING_INTERVAL = 25.seconds
    }
}
