package eu.kanade.tachiyomi.source.entry

import okhttp3.Headers
import okhttp3.OkHttpClient

/**
 * Optional capability for sources whose covers and chapter stills need the source's own client or
 * headers, for example a host that only serves images with the site's Referer or cookies.
 *
 * Katari loads artwork of sources without it with its default client and no source headers.
 */
interface ArtworkSource : UnifiedSource {

    /** Client used for cover and still requests. */
    val client: OkHttpClient

    /** Headers sent with cover and still requests. */
    val headers: Headers
}
