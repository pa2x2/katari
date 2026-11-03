package eu.kanade.tachiyomi.network

import android.content.Context
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.CloudflareRequestSnapshotInterceptor
import eu.kanade.tachiyomi.network.interceptor.WebViewInterceptorMessages
import eu.kanade.tachiyomi.network.localnetwork.guardingLocalNetworkAccess
import okhttp3.OkHttpClient

actual class NetworkHelper(
    private val context: Context,
    private val preferences: NetworkPreferences,
    private val webViewMessages: WebViewInterceptorMessages,
    private val localNetworkAccessDeniedMessage: (host: String) -> String,
) {

    actual val cookieJar = AndroidCookieJar()

    // Building it reads preferences and sets up the cache and DNS, which a process start that never makes a request
    // shouldn't pay for
    actual val client: OkHttpClient by lazy {
        sourceClientBuilder(
            cookieJar = cookieJar,
            cacheDirectory = context.cacheDir,
            preferences = preferences,
            userAgent = ::defaultUserAgentProvider,
            networkInterceptors = listOf(CloudflareRequestSnapshotInterceptor()),
        )
            .addInterceptor(
                CloudflareInterceptor(context, cookieJar, ::defaultUserAgentProvider, webViewMessages),
            )
            .build()
            .guardingLocalNetworkAccess(context, localNetworkAccessDeniedMessage)
    }

    @Deprecated("The regular client handles Cloudflare by default")
    @Suppress("UNUSED")
    actual val cloudflareClient: OkHttpClient get() = client

    actual fun defaultUserAgentProvider() = preferences.defaultUserAgent.get().trim()
}
