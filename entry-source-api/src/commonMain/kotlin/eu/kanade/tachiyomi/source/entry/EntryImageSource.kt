package eu.kanade.tachiyomi.source.entry

import eu.kanade.tachiyomi.network.ProgressListener
import eu.kanade.tachiyomi.network.awaitSuccess
import eu.kanade.tachiyomi.network.newCachelessCallWithProgress
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response

/**
 * Optional capability for sources that can load manga-like image pages.
 */
interface EntryImageSource : UnifiedSource {

    /**
     * Default network client for image requests.
     */
    val client: OkHttpClient

    /**
     * Default headers for image requests.
     */
    val headers: Headers

    /**
     * Resolves the final image URL for [page].
     */
    suspend fun getImageUrl(page: EntryImagePage): String = page.imageUrl ?: page.url

    /**
     * Builds the request used to download the resolved image URL.
     */
    fun imageRequest(page: EntryImagePage, imageUrl: String): Request

    /**
     * Downloads the image for [page].
     */
    suspend fun getImage(page: EntryImagePage, progress: ProgressListener? = null): Response {
        val imageUrl = page.imageUrl ?: getImageUrl(page)
        val request = imageRequest(page, imageUrl)
        val listener = progress ?: object : ProgressListener {
            override fun update(bytesRead: Long, contentLength: Long, done: Boolean) = Unit
        }
        return client.newCachelessCallWithProgress(request, listener).awaitSuccess()
    }
}
