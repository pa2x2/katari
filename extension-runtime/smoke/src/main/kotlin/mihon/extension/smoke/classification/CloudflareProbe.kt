package mihon.extension.smoke.classification

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.interceptor.isCloudflareChallenge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * Whether a site answers with a Cloudflare challenge. On desktop the network passes a challenge on unsolved, so the
 * source then fails in its own way: an HTTP status, a script's message, a page it can't read. Asking the site again
 * tells that apart from the site's other errors. Each host is asked once per run.
 */
internal class CloudflareProbe(client: OkHttpClient, timeout: Duration) {
    private val client = client.newBuilder().callTimeout(timeout.toJavaDuration()).build()
    private val answers = ConcurrentHashMap<String, Boolean>()

    suspend fun isChallenged(url: String): Boolean {
        val host = url.toHttpUrlOrNull()?.host ?: return false
        answers[host]?.let { return it }
        val challenged = withContext(Dispatchers.IO) {
            try {
                client.newCall(GET(url)).execute().use { it.isCloudflareChallenge() }
            } catch (_: IOException) {
                false
            }
        }
        answers[host] = challenged
        return challenged
    }
}
