package eu.kanade.tachiyomi.data.coil

import coil3.Extras
import coil3.ImageLoader
import coil3.fetch.Fetcher
import coil3.key.Keyer
import coil3.network.NetworkHeaders
import coil3.network.httpHeaders
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.Options
import coil3.toUri
import mihon.entry.interactions.source.EntryCoverNetworkFeature
import mihon.entry.interactions.source.EntryCoverNetworkResolution
import okhttp3.Call

/**
 * An image a source serves besides its covers, such as an episode's still or an entry's background art. Many sources
 * only serve images to requests carrying their own headers, such as a referer.
 */
data class SourceImage(val sourceId: Long, val url: String)

/**
 * Loads a [SourceImage] like any network image, through Coil's disk cache, but with its source's client and headers.
 * Unlike covers it isn't kept in the cover cache, so it is gone once the disk cache evicts it.
 */
class SourceImageFetcherFactory(
    private val defaultCallFactory: Lazy<Call.Factory>,
    private val networkFeature: Lazy<EntryCoverNetworkFeature>,
) : Fetcher.Factory<SourceImage> {

    override fun create(data: SourceImage, options: Options, imageLoader: ImageLoader): Fetcher? {
        val access = when (val resolution = networkFeature.value.resolve(data.sourceId)) {
            is EntryCoverNetworkResolution.Available -> resolution
            is EntryCoverNetworkResolution.Failed -> throw resolution.cause
            is EntryCoverNetworkResolution.Missing, is EntryCoverNetworkResolution.Unsupported -> null
        }
        val headers = NetworkHeaders.Builder(options.httpHeaders).apply {
            access?.headers?.forEach { (name, value) -> set(name, value) }
        }.build()
        val sourceOptions = options.copy(
            extras = options.extras.newBuilder().set(Extras.Key.httpHeaders, headers).build(),
        )
        val callFactory = access?.callFactory ?: defaultCallFactory.value
        return OkHttpNetworkFetcherFactory(callFactory = { callFactory })
            .create(data.url.toUri(), sourceOptions, imageLoader)
    }
}

class SourceImageKeyer : Keyer<SourceImage> {
    override fun key(data: SourceImage, options: Options): String = "${data.sourceId};${data.url}"
}
