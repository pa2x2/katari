package mihon.entry.interactions.anime.stream

import eu.kanade.tachiyomi.source.entry.VideoAudioTrack
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType

/*
 * A stream's type as far as it can be told without a request: the source's type, its MIME type, then the URL's ending.
 * It ranks streams before one is picked; the picked one is probed ([StreamTypeProbe]), which overrides the URL's
 * ending, since hosts serve playlists under any name.
 */

internal fun declaredStreamType(stream: VideoStream): VideoStreamType =
    declaredStreamType(stream.type, stream.mimeType, stream.request?.url)

internal fun declaredStreamType(track: VideoAudioTrack): VideoStreamType =
    declaredStreamType(track.type, track.mimeType, track.request.url)

/** Whether the source said what the media is, so it needn't be probed. */
internal fun isTypedBySource(type: VideoStreamType, mimeType: String?): Boolean =
    type != VideoStreamType.UNKNOWN || typeOfMime(mimeType) != VideoStreamType.UNKNOWN

private fun declaredStreamType(type: VideoStreamType, mimeType: String?, url: String?): VideoStreamType {
    if (type != VideoStreamType.UNKNOWN) return type
    typeOfMime(mimeType).takeIf { it != VideoStreamType.UNKNOWN }?.let { return it }
    val path = url.orEmpty().substringBefore('#').substringBefore('?').lowercase()
    return when {
        path.endsWith(".m3u8") || path.endsWith(".m3u") -> VideoStreamType.HLS
        path.endsWith(".mpd") -> VideoStreamType.DASH
        PROGRESSIVE_EXTENSIONS.any(path::endsWith) -> VideoStreamType.PROGRESSIVE
        else -> VideoStreamType.UNKNOWN
    }
}

private fun typeOfMime(mimeType: String?): VideoStreamType {
    val mime = mimeType.orEmpty().lowercase()
    return when {
        mime.isEmpty() -> VideoStreamType.UNKNOWN
        "mpegurl" in mime || "m3u8" in mime -> VideoStreamType.HLS
        "dash+xml" in mime -> VideoStreamType.DASH
        mime.startsWith("video/") || mime.startsWith("audio/") || "mp4" in mime -> VideoStreamType.PROGRESSIVE
        else -> VideoStreamType.UNKNOWN
    }
}

private val PROGRESSIVE_EXTENSIONS = listOf(".mp4", ".m4v", ".mkv", ".webm", ".mov", ".m4a", ".mp3", ".aac")
