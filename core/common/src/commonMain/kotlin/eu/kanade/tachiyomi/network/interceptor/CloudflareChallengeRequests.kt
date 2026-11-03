// Extensions compiled against the SDK link against the functions here in the class of Android's interceptor file
@file:JvmName("CloudflareInterceptorKt")

package eu.kanade.tachiyomi.network.interceptor

import okhttp3.Cookie
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup

private val ERROR_CODES = listOf(403, 503)
private const val MAX_LEGACY_CHALLENGE_BODY_BYTES = 64L * 1024
private val SERVER_CHECK = setOf("cloudflare-nginx", "cloudflare")

/**
 * Whether this is a Cloudflare challenge page, which only a web view gets through. Public for developer tools that tell
 * a source failing for want of one from a site's other errors.
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

/** Whether a page's HTTP error, [statusCode] with [headers], may still be the challenge, which then runs on. */
internal fun isCloudflareChallengeError(statusCode: Int, headers: Map<String, String>): Boolean =
    statusCode in ERROR_CODES || isCloudflareChallengeResponse(headers)

/** Whether a response with [headers] is Cloudflare's challenge, as Cloudflare marks it. */
internal fun isCloudflareChallengeResponse(headers: Map<String, String>): Boolean =
    headers.any { (name, value) ->
        name.equals("cf-mitigated", ignoreCase = true) && value.equals("challenge", ignoreCase = true)
    }

/**
 * Notes the URL and clearance each request [withCloudflareRequestSnapshot] goes out with, as the network sends them,
 * after redirects and with the cookie jar's cookies.
 */
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

internal fun Request.cloudflareClearanceValue(): String? = headers.values("Cookie")
    .asSequence()
    .flatMap { it.splitToSequence(';') }
    .map(String::trim)
    .firstOrNull { it.substringBefore('=').trim() == "cf_clearance" }
    ?.substringAfter('=', "")
    ?.trim()
    ?.ifBlank { null }

/**
 * Whether the clearance the jar holds now, [currentClearance], came after the one the challenged request sent: another
 * request solved this challenge meanwhile, and its clearance is worth a try before another solve.
 */
internal fun shouldReuseCloudflareClearance(
    sentRequest: CloudflareSentRequest?,
    challengeUrl: HttpUrl,
    currentClearance: Cookie?,
): Boolean = sentRequest?.url == challengeUrl &&
    currentClearance != null &&
    currentClearance.value != sentRequest.clearance
