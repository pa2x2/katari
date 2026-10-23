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
import eu.kanade.tachiyomi.source.entry.VideoSubtitle

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
    subtitles: List<VideoSubtitle>,
): DefaultMediaSourceFactory {
    val requestHeaders = stream.request.headers
    val streamUri = Uri.parse(stream.request.url)
    val userAgent = requestHeaders.entries.firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }?.value
        ?: networkHelper.defaultUserAgentProvider()

    val okHttpDataSourceFactory = OkHttpDataSource.Factory(networkHelper.client)
        .setUserAgent(userAgent)
        .setDefaultRequestProperties(requestHeaders)
    val subtitleHeadersByUrl = subtitles.associate { it.request.url to it.request.headers }
    val defaultDataSourceFactory = DefaultDataSource.Factory(context, okHttpDataSourceFactory)
    val resolvedDataSourceFactory = ResolvingDataSource.Factory(defaultDataSourceFactory) { dataSpec ->
        val resolvedUri = rewriteDownloadedHlsContentUri(baseUri = streamUri, candidateUri = dataSpec.uri)
        val resolvedDataSpec = if (resolvedUri == dataSpec.uri) {
            dataSpec
        } else {
            dataSpec.buildUpon().setUri(resolvedUri).build()
        }
        val headers = subtitleHeadersByUrl[resolvedUri.toString()].orEmpty()
        if (headers.isEmpty()) {
            resolvedDataSpec
        } else {
            resolvedDataSpec.withAdditionalHeaders(headers)
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

private fun rewriteDownloadedHlsContentUri(baseUri: Uri, candidateUri: Uri): Uri {
    if (baseUri.scheme != "content" || candidateUri.scheme != "content") return candidateUri

    val baseDocumentId = runCatching { DocumentsContract.getDocumentId(baseUri) }.getOrNull() ?: return candidateUri
    val candidateDocumentId =
        runCatching { DocumentsContract.getDocumentId(candidateUri) }.getOrNull() ?: return candidateUri
    val baseParentDocumentId = baseDocumentId.substringBeforeLast('/', missingDelimiterValue = "")
    if (baseParentDocumentId.isBlank() || candidateDocumentId.startsWith("$baseParentDocumentId/")) {
        return candidateUri
    }

    return DocumentsContract.buildDocumentUriUsingTree(
        baseUri,
        "$baseParentDocumentId/$candidateDocumentId",
    )
}
