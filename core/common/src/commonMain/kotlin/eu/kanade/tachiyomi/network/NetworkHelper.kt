package eu.kanade.tachiyomi.network

import eu.kanade.tachiyomi.network.interceptor.SourcePreferenceProfileForwarding
import eu.kanade.tachiyomi.network.interceptor.UncaughtExceptionInterceptor
import eu.kanade.tachiyomi.network.interceptor.UserAgentInterceptor
import okhttp3.Cache
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import java.io.File
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

/**
 * The HTTP clients sources use. Extensions link against this class and [AndroidCookieJar] by name on every platform,
 * so each platform keeps both names and their members; only construction differs.
 */
expect class NetworkHelper {
    val cookieJar: AndroidCookieJar

    val client: OkHttpClient

    /**
     * @deprecated Since extension-lib 1.5
     */
    @Deprecated("The regular client handles Cloudflare by default")
    val cloudflareClient: OkHttpClient

    fun defaultUserAgentProvider(): String
}

/**
 * Everything both platforms' clients share. [networkInterceptors] run before the verbose-logging interceptor, so it
 * logs what they leave.
 */
internal fun sourceClientBuilder(
    cookieJar: AndroidCookieJar,
    cacheDirectory: File,
    preferences: NetworkPreferences,
    userAgent: () -> String,
    networkInterceptors: List<Interceptor>,
): OkHttpClient.Builder {
    val builder = OkHttpClient.Builder()
        .cookieJar(cookieJar)
        .connectTimeout(30.seconds)
        .readTimeout(30.seconds)
        .callTimeout(2.minutes)
        .cache(
            Cache(
                directory = File(cacheDirectory, "network_cache"),
                maxSize = 5L * 1024 * 1024, // 5 MiB
            ),
        )
        .eventListener(SourcePreferenceProfileForwarding)
        .addInterceptor(UncaughtExceptionInterceptor())
        .addInterceptor(SourcePreferenceProfileForwarding)
        .addInterceptor(UserAgentInterceptor(userAgent))
    networkInterceptors.forEach(builder::addNetworkInterceptor)

    if (preferences.verboseLogging.get()) {
        val httpLoggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
            if (preferences.redactSensitiveHeaders.get()) {
                redactHeader("Authorization")
                redactHeader("Auth")
            }
        }
        builder.addNetworkInterceptor(httpLoggingInterceptor)
    }

    return when (preferences.dohProvider.get()) {
        PREF_DOH_CLOUDFLARE -> builder.dohCloudflare()
        PREF_DOH_GOOGLE -> builder.dohGoogle()
        PREF_DOH_ADGUARD -> builder.dohAdGuard()
        PREF_DOH_QUAD9 -> builder.dohQuad9()
        PREF_DOH_ALIDNS -> builder.dohAliDNS()
        PREF_DOH_DNSPOD -> builder.dohDNSPod()
        PREF_DOH_360 -> builder.doh360()
        PREF_DOH_QUAD101 -> builder.dohQuad101()
        PREF_DOH_MULLVAD -> builder.dohMullvad()
        PREF_DOH_CONTROLD -> builder.dohControlD()
        PREF_DOH_NJALLA -> builder.dohNajalla()
        PREF_DOH_SHECAN -> builder.dohShecan()
        else -> builder
    }
}
