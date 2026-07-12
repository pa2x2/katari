package eu.kanade.tachiyomi.source.entry

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.network.NetworkHelper
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import uy.kohesive.injekt.injectLazy
import java.net.URI
import java.net.URISyntaxException
import java.security.MessageDigest

/**
 * Thin HTTP base for Entry-era catalogue and video sources.
 *
 * Use [EntryImageHttpSource] for manga-like HTTP sources that can load image
 * pages and expose chapter URLs.
 */
abstract class EntryHttpSource :
    EntryCatalogueSource,
    WebViewSource,
    SourceHomePage {

    /**
     * Network service.
     */
    protected val network: NetworkHelper by injectLazy()

    /**
     * Base URL of the website without a trailing slash, e.g. `https://example.org`.
     */
    abstract val baseUrl: String

    /**
     * Version ID used to generate the source ID.
     */
    open val versionId: Int = 1

    override val id: Long by lazy { generateId(name, lang, versionId) }

    override val supportsLatest: Boolean = false

    open val headers: Headers by lazy { headersBuilder().build() }

    open val client: OkHttpClient get() = network.client

    override fun getHomeUrl(): String = baseUrl

    override fun getContentUrl(entry: SEntry): String = baseUrl + entry.url

    override fun getWebViewHeaders(): Map<String, String> =
        headers.toMultimap().mapValues { it.value.getOrNull(0) ?: "" }

    /**
     * Headers builder for requests. Implementations can override this method for custom headers.
     */
    protected open fun headersBuilder(): Headers.Builder = Headers.Builder().apply {
        add("User-Agent", network.defaultUserAgentProvider())
    }

    /**
     * Generates a unique source ID using the legacy HttpSource-compatible algorithm.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    protected fun generateId(name: String, lang: String, versionId: Int): Long {
        val key = "${name.lowercase()}/$lang/$versionId"
        val bytes = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        return (0..7).map { bytes[it].toLong() and 0xff shl 8 * (7 - it) }.reduce(Long::or) and Long.MAX_VALUE
    }

    /**
     * Assigns the URL of the entry without the scheme and domain.
     */
    fun SEntry.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    /**
     * Returns [orig] without scheme and domain.
     */
    protected fun getUrlWithoutDomain(orig: String): String {
        return try {
            val uri = URI(orig.replace(" ", "%20"))
            var out = uri.path
            if (uri.query != null) {
                out += "?" + uri.query
            }
            if (uri.fragment != null) {
                out += "#" + uri.fragment
            }
            out
        } catch (_: URISyntaxException) {
            orig
        }
    }

    /**
     * Visible name of the source.
     */
    override fun toString(): String = "$name (${lang.uppercase()})"
}

/**
 * HTTP base for manga-like Entry sources that load image pages.
 */
abstract class EntryImageHttpSource :
    EntryHttpSource(),
    EntryImageSource,
    ChapterWebViewSource {

    /**
     * Assigns the URL of the chapter without the scheme and domain.
     */
    fun SEntryChapter.setUrlWithoutDomain(url: String) {
        this.url = getUrlWithoutDomain(url)
    }

    override fun getChapterUrl(chapter: SEntryChapter): String = baseUrl + chapter.url

    override fun imageRequest(page: EntryImagePage, imageUrl: String): Request =
        GET(imageUrl, headers)
}
