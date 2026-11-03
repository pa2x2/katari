package eu.kanade.tachiyomi.network

import android.content.Context
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.CloudflareRequestSnapshotInterceptor
import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import okhttp3.OkHttpClient
import java.io.File
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * The client is laid out as Android's, with its [CloudflareInterceptor], since extensions that rebuild it check for the
 * interceptors they expect by name. Where the app has no challenge pages, that interceptor passes challenges on
 * unsolved and tells the user, through [context]'s toasts, that the source needs a web view.
 *
 * @param trustManager what servers are checked with, as Android's clients check them with the system's authorities;
 * null for the JDK's.
 */
actual class NetworkHelper(
    private val context: Context,
    private val cacheDirectory: File,
    private val preferences: NetworkPreferences,
    private val webViewMessages: WebViewInterceptorMessages,
    private val trustManager: X509TrustManager?,
) {

    /**
     * Android's constructor, kept for extensions that link against it. Desktop has no local network permission to
     * report, so this builds the same client, cached in the context's cache folder, checking servers with the JDK's
     * authorities.
     */
    constructor(
        context: Context,
        preferences: NetworkPreferences,
        webViewMessages: WebViewInterceptorMessages,
        localNetworkAccessDeniedMessage: (host: String) -> String,
    ) : this(context, context.cacheDir, preferences, webViewMessages, trustManager = null)

    actual val cookieJar = AndroidCookieJar()

    actual val client: OkHttpClient by lazy {
        sourceClientBuilder(
            cookieJar = cookieJar,
            cacheDirectory = cacheDirectory,
            preferences = preferences,
            userAgent = ::defaultUserAgentProvider,
            networkInterceptors = listOf(CloudflareRequestSnapshotInterceptor()),
        )
            .addInterceptor(CloudflareInterceptor(context, cookieJar, ::defaultUserAgentProvider, webViewMessages))
            .apply {
                if (trustManager != null) {
                    val tls = SSLContext.getInstance("TLS").apply { init(null, arrayOf(trustManager), null) }
                    sslSocketFactory(tls.socketFactory, trustManager)
                }
            }
            .build()
    }

    @Deprecated("The regular client handles Cloudflare by default")
    actual val cloudflareClient: OkHttpClient get() = client

    actual fun defaultUserAgentProvider() = preferences.defaultUserAgent.get().trim()
}
