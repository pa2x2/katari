package eu.kanade.tachiyomi.network

import android.content.Context
import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import okhttp3.OkHttpClient
import java.io.File

/** Without a web view yet, Cloudflare challenges are not solved on desktop. */
actual class NetworkHelper(
    private val cacheDirectory: File,
    private val preferences: NetworkPreferences,
) {

    /**
     * Android's constructor, kept for extensions that link against it. Desktop has neither a web view to solve
     * challenges in nor a local network permission to report, so this builds the same client, cached in the
     * context's cache folder.
     */
    constructor(
        context: Context,
        preferences: NetworkPreferences,
        webViewMessages: WebViewInterceptorMessages,
        localNetworkAccessDeniedMessage: (host: String) -> String,
    ) : this(context.cacheDir, preferences)

    actual val cookieJar = AndroidCookieJar()

    actual val client: OkHttpClient by lazy {
        sourceClientBuilder(
            cookieJar = cookieJar,
            cacheDirectory = cacheDirectory,
            preferences = preferences,
            userAgent = ::defaultUserAgentProvider,
            networkInterceptors = emptyList(),
        ).build()
    }

    @Deprecated("The regular client handles Cloudflare by default")
    actual val cloudflareClient: OkHttpClient get() = client

    actual fun defaultUserAgentProvider() = preferences.defaultUserAgent.get().trim()
}
