package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.VideoStream
import java.io.IOException

/*
 * Katari has no torrent engine. Some sources offer an episode as a magnet link or a .torrent file, often only until the
 * user sets up a debrid service in the source's settings; such a stream is never tried, and when it is all a source
 * offers the viewer is told so instead of seeing a generic failure.
 */

internal val VideoStream.isTorrent: Boolean
    get() {
        if (mimeType.equals(TORRENT_MIME_TYPE, ignoreCase = true)) return true
        val url = request?.url?.trim() ?: return false
        return url.startsWith(MAGNET_SCHEME, ignoreCase = true) ||
            url.substringBefore('#').substringBefore('?').endsWith(TORRENT_EXTENSION, ignoreCase = true)
    }

/** A stream resolved on demand turned out to be a torrent. */
internal class TorrentStreamException(streamKey: String) : IOException("Stream '$streamKey' is a torrent")

private const val MAGNET_SCHEME = "magnet:"
private const val TORRENT_EXTENSION = ".torrent"
private const val TORRENT_MIME_TYPE = "application/x-bittorrent"
