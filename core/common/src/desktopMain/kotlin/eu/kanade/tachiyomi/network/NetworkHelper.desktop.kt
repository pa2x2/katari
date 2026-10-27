package eu.kanade.tachiyomi.network

import okhttp3.OkHttpClient
import java.io.File

/** Without a web view yet, Cloudflare challenges are not solved on desktop. */
actual class NetworkHelper(
    private val cacheDirectory: File,
    private val preferences: NetworkPreferences,
) {

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
