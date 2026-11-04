package mihon.video.engine.spi.loading

import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream

/**
 * The request of a stream the host has already resolved. Sources may list streams without a request and resolve them
 * on demand; only resolved streams reach the engines, the downloader and the feed.
 */
val VideoStream.resolvedRequest: VideoRequest
    get() = checkNotNull(request) { "Stream '$key' reached playback before it was resolved" }
