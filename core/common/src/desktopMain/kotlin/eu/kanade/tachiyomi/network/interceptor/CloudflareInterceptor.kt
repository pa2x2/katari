package eu.kanade.tachiyomi.network.interceptor

import android.content.Context
import eu.kanade.tachiyomi.network.AndroidCookieJar
import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import java.util.concurrent.ConcurrentHashMap

/**
 * Android's Cloudflare interceptor, kept for extensions that build their own client with it. Solving a challenge takes
 * a web view, which desktop doesn't have yet, so [WebViewInterceptor] passes challenges on unsolved. The parts that
 * need none, from telling a challenge to the coordinator solves of one zone share, are Android's.
 */
class CloudflareInterceptor(
    context: Context,
    cookieManager: AndroidCookieJar,
    defaultUserAgentProvider: () -> String,
    messages: WebViewInterceptorMessages,
) : WebViewInterceptor(context, defaultUserAgentProvider, messages) {

    override fun prepareRequest(request: Request): Request {
        return request.withCloudflareRequestSnapshot()
    }

    override fun shouldIntercept(response: Response): Boolean {
        return response.isCloudflareChallenge()
    }

    override fun intercept(
        chain: Interceptor.Chain,
        request: Request,
        response: Response,
    ): Response = response
}

private val ERROR_CODES = listOf(403, 503)
private const val MAX_LEGACY_CHALLENGE_BODY_BYTES = 64L * 1024
private val SERVER_CHECK = setOf("cloudflare-nginx", "cloudflare")

internal class CloudflareRequestSnapshotInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        request.tag(CloudflareRequestState::class.java)?.sentRequest = CloudflareSentRequest(
            url = request.url,
            clearance = request.cloudflareClearanceValue(),
        )
        return chain.proceed(request)
    }
}

internal class CloudflareRequestState {
    @Volatile
    var sentRequest: CloudflareSentRequest? = null
}

internal data class CloudflareSentRequest(
    val url: HttpUrl,
    val clearance: String?,
)

internal fun Request.withCloudflareRequestSnapshot(): Request = newBuilder()
    .tag(CloudflareRequestState::class.java, CloudflareRequestState())
    .build()

internal fun Request.cloudflareSentRequest(): CloudflareSentRequest? =
    tag(CloudflareRequestState::class.java)?.sentRequest

/**
 * Whether this is a Cloudflare challenge page, which only a web view gets through. Public on desktop for developer tools
 * that tell a source failing for want of one from a site's other errors.
 */
fun Response.isCloudflareChallenge(): Boolean {
    if (header("cf-mitigated").equals("challenge", ignoreCase = true)) {
        return true
    }

    val isLegacyCloudflareResponse = code in ERROR_CODES &&
        SERVER_CHECK.any { server -> header("Server").equals(server, ignoreCase = true) }
    if (!isLegacyCloudflareResponse) {
        return false
    }

    val document = Jsoup.parse(
        peekBody(MAX_LEGACY_CHALLENGE_BODY_BYTES).string(),
        request.url.toString(),
    )

    return document.getElementById("challenge-error-title") != null ||
        document.getElementById("challenge-error-text") != null
}

internal class CloudflareChallengeCoordinator(
    private val zoneFor: (HttpUrl) -> String = { it.topPrivateDomain() ?: it.host },
    private val onRegistered: (HttpUrl) -> Unit = {},
) {
    private val locks = ConcurrentHashMap<String, LockEntry>()

    fun solve(url: HttpUrl, block: () -> Unit) {
        val zone = zoneFor(url)
        val entry = locks.compute(zone) { _, current ->
            (current ?: LockEntry()).also { it.users++ }
        }!!

        try {
            onRegistered(url)
            synchronized(entry.monitor) {
                block()
            }
        } finally {
            locks.computeIfPresent(zone) { _, current ->
                check(current === entry)
                current.users--
                current.takeIf { it.users > 0 }
            }
        }
    }

    private class LockEntry(
        val monitor: Any = Any(),
        var users: Int = 0,
    )
}

internal fun Request.cloudflareClearanceValue(): String? = headers.values("Cookie")
    .asSequence()
    .flatMap { it.splitToSequence(';') }
    .map(String::trim)
    .firstOrNull { it.substringBefore('=').trim() == "cf_clearance" }
    ?.substringAfter('=', "")
    ?.trim()
    ?.ifBlank { null }

internal fun shouldReuseCloudflareClearance(
    sentRequest: CloudflareSentRequest?,
    challengeUrl: HttpUrl,
    currentClearance: Cookie?,
): Boolean = sentRequest?.url == challengeUrl &&
    currentClearance != null &&
    currentClearance.value != sentRequest.clearance
