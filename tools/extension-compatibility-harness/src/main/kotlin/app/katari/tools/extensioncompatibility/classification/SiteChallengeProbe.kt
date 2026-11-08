package app.katari.tools.extensioncompatibility.classification

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.CloudflareInterceptor
import eu.kanade.tachiyomi.network.interceptor.isCloudflareChallenge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Response
import java.io.IOException
import java.util.Optional
import java.util.concurrent.ConcurrentHashMap
import kotlin.jvm.optionals.getOrNull
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Which bot check, if any, a site answers with: a page only a browser gets through, by running its script, so on Android
 * too the source works once the user has opened the site in the web view. The app solves Cloudflare's on its own; any
 * other reaches the source, which then fails in its own way: an HTTP status, a script's own message, a page it can't
 * read or that has no entries. Asking the site again, without the app's solver, tells that apart from the site's other
 * errors. Each host is asked once per run.
 */
internal class SiteChallengeProbe(client: OkHttpClient, timeout: Duration) {
    private val client = client.newBuilder()
        .apply { interceptors().removeAll { it is CloudflareInterceptor } }
        .callTimeout(timeout.toJavaDuration())
        .build()
    private val answers = ConcurrentHashMap<String, Optional<String>>()

    /** The bot check's name, or null when the site answers without one. */
    suspend fun challengeOf(url: String): String? {
        val host = url.toHttpUrlOrNull()?.host ?: return null
        answers[host]?.let { return it.getOrNull() }
        val challenge = withContext(Dispatchers.IO) {
            try {
                client.newCall(GET(url)).execute().use { it.challenge() }
            } catch (_: IOException) {
                null
            }
        }
        answers[host] = Optional.ofNullable(challenge)
        return challenge
    }

    private fun Response.challenge(): String? {
        if (isCloudflareChallenge()) return "Cloudflare"
        // Anubis's proof-of-work page sets its cookie and carries its challenge as JSON for its script
        if (headers("Set-Cookie").any { ANUBIS_COOKIE in it }) return "Anubis"
        val page = peekBody(MAX_PAGE_BYTES).string()
        if (ANUBIS_CHALLENGE in page) return "Anubis"
        // A page whose only content is a script sending the browser back with the token it computed
        if (page.length < MAX_SCRIPT_REDIRECT_LENGTH && SCRIPT_TOKEN_REDIRECT.containsMatchIn(page)) {
            return "JavaScript redirect"
        }
        return null
    }

    private companion object {
        const val MAX_PAGE_BYTES = 256L * 1024
        const val ANUBIS_COOKIE = "-anubis-"
        const val ANUBIS_CHALLENGE = "id=\"anubis_challenge\""
        const val MAX_SCRIPT_REDIRECT_LENGTH = 2048
        val SCRIPT_TOKEN_REDIRECT = Regex("""window\.location\.replace\('[^']*[?&]js=ey""")
    }
}
