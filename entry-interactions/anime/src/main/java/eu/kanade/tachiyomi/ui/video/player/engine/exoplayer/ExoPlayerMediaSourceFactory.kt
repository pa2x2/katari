package eu.kanade.tachiyomi.ui.video.player.engine.exoplayer

import android.content.Context
import android.net.Uri
import android.provider.DocumentsContract
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import eu.kanade.tachiyomi.network.NetworkHelper
import eu.kanade.tachiyomi.source.entry.VideoStream
import mihon.video.engine.spi.loading.VideoRequestHeaders
import mihon.video.engine.spi.loading.resolvedRequest
import mihon.video.engine.spi.loading.siblingDocumentUri

/**
 * Where the ExoPlayer engine loads a stream from: OkHttp with the stream's headers and user agent, then
 * [rewriteDownloadedHlsContentUri] for downloaded HLS and the per-subtitle headers, then the media cache for http(s)
 * streams.
 */
@OptIn(markerClass = [UnstableApi::class])
internal fun buildExoPlayerMediaSourceFactory(
    context: Context,
    networkHelper: NetworkHelper,
    mediaCache: ExoPlayerMediaCache,
    stream: VideoStream,
): DefaultMediaSourceFactory {
    val headers = VideoRequestHeaders(stream, networkHelper::defaultUserAgentProvider)
    val streamUri = Uri.parse(stream.resolvedRequest.url)

    val okHttpDataSourceFactory = OkHttpDataSource.Factory(networkHelper.client)
        .setUserAgent(headers.userAgent)
        .setDefaultRequestProperties(headers.stream)
    val defaultDataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)
    val resolvedDataSourceFactory = ResolvingDataSource.Factory(defaultDataSourceFactory) { dataSpec ->
        val resolvedUri = rewriteDownloadedHlsContentUri(baseUri = streamUri, candidateUri = dataSpec.uri)
        val resolvedDataSpec = if (resolvedUri == dataSpec.uri) {
            dataSpec
        } else {
            dataSpec.buildUpon().setUri(resolvedUri).build()
        }
        val subtitleHeaders = headers.subtitleExtras(resolvedUri.toString())
        if (subtitleHeaders.isEmpty()) {
            resolvedDataSpec
        } else {
            resolvedDataSpec.withAdditionalHeaders(subtitleHeaders)
        }
    }
    val dataSourceFactory: DataSource.Factory = if (streamUri.scheme.equals("http", ignoreCase = true) ||
        streamUri.scheme.equals("https", ignoreCase = true)
    ) {
        CacheDataSource.Factory()
            .setCache(mediaCache.cache)
            .setUpstreamDataSourceFactory(resolvedDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)
    } else {
        resolvedDataSourceFactory
    }

    return DefaultMediaSourceFactory(dataSourceFactory)
}

/**
 * ExoPlayer resolves a downloaded playlist's relative reference against the playlist's document URI, which yields a
 * document ID of just the file name; the file is the playlist's sibling in the download's tree.
 */
private fun rewriteDownloadedHlsContentUri(baseUri: Uri, candidateUri: Uri): Uri {
    if (baseUri.scheme != "content" || candidateUri.scheme != "content") return candidateUri
    val baseDocumentId = runCatching { DocumentsContract.getDocumentId(baseUri) }.getOrNull() ?: return candidateUri
    val candidateDocumentId =
        runCatching { DocumentsContract.getDocumentId(candidateUri) }.getOrNull() ?: return candidateUri
    val baseParentDocumentId = baseDocumentId.substringBeforeLast('/', missingDelimiterValue = "")
    if (candidateDocumentId.startsWith("$baseParentDocumentId/")) return candidateUri
    return siblingDocumentUri(baseUri, candidateDocumentId) ?: candidateUri
}
