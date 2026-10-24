package eu.kanade.tachiyomi.source.aniyomi.playback

import eu.kanade.tachiyomi.animesource.model.ChapterType
import eu.kanade.tachiyomi.animesource.model.TimeStamp
import eu.kanade.tachiyomi.animesource.model.Track
import eu.kanade.tachiyomi.animesource.model.Video
import eu.kanade.tachiyomi.source.entry.PlaybackMarker
import eu.kanade.tachiyomi.source.entry.PlaybackMarkerKind
import eu.kanade.tachiyomi.source.entry.VideoAudioTrack
import eu.kanade.tachiyomi.source.entry.VideoRequest
import eu.kanade.tachiyomi.source.entry.VideoStream
import eu.kanade.tachiyomi.source.entry.VideoStreamType
import eu.kanade.tachiyomi.source.entry.VideoSubtitle
import okhttp3.Headers

/**
 * [videos] as streams keyed by their titles, which stay the same from one episode to the next, so the stream a viewer
 * picked is found again. [sourceHeaders] go with a video that has none of its own, as in Aniyomi's player.
 */
internal fun streamsOf(videos: List<Video>, sourceHeaders: Headers?): List<VideoStream> {
    val keys = distinctKeys(videos.map { it.videoTitle })
    return videos.mapIndexed { index, video -> video.toVideoStream(keys[index], video.headers ?: sourceHeaders) }
}

/** A video without a link yet: extensions-lib 14 writes `"null"` where `getVideoUrl` has to find it. */
internal val Video.needsLink: Boolean
    get() = videoUrl.isBlank() || videoUrl == UNRESOLVED_LIB14_URL

internal fun Video.toVideoStream(key: String, headers: Headers?): VideoStream {
    val headerMap = headers?.toMap().orEmpty()
    return VideoStream(
        request = if (needsLink) null else VideoRequest(videoUrl, headerMap),
        label = videoTitle,
        type = streamType(videoUrl),
        key = key,
        height = resolution,
        bitrate = bitrate?.toLong(),
        subtitles = subtitleTracks.mapIndexed { index, track -> track.toSubtitle(index, headerMap) },
        audioTracks = audioTracks.mapIndexed { index, track -> track.toAudioTrack(index, headerMap) },
        markers = timestamps.mapNotNull { it.toMarker() },
        mpvOptions = mpvArgs.toMap(),
    )
}

private fun Track.toSubtitle(index: Int, headers: Map<String, String>) = VideoSubtitle(
    request = VideoRequest(url, headers),
    label = lang,
    key = "$index:$lang",
)

private fun Track.toAudioTrack(index: Int, headers: Map<String, String>) = VideoAudioTrack(
    request = VideoRequest(url, headers),
    label = lang,
    type = streamType(url),
    key = "$index:$lang",
)

private fun TimeStamp.toMarker(): PlaybackMarker? {
    val startMs = (start * MS_PER_SECOND).toLong()
    val endMs = (end * MS_PER_SECOND).toLong()
    if (endMs <= startMs) return null
    val kind = when (type) {
        ChapterType.Opening -> PlaybackMarkerKind.INTRO
        ChapterType.Ending -> PlaybackMarkerKind.OUTRO
        ChapterType.Recap -> PlaybackMarkerKind.RECAP
        ChapterType.MixedOp -> PlaybackMarkerKind.MIXED_INTRO
        ChapterType.Other -> PlaybackMarkerKind.CHAPTER
    }
    return PlaybackMarker(startMs, endMs, kind, label = name.takeIf(String::isNotBlank))
}

private fun streamType(url: String): VideoStreamType {
    val path = url.substringBefore('#').substringBefore('?')
    return when {
        path.endsWith(".m3u8", ignoreCase = true) -> VideoStreamType.HLS
        path.endsWith(".mpd", ignoreCase = true) -> VideoStreamType.DASH
        else -> VideoStreamType.UNKNOWN
    }
}

private fun Headers.toMap(): Map<String, String> = associate { (name, value) -> name to value }

private const val UNRESOLVED_LIB14_URL = "null"
private const val MS_PER_SECOND = 1000
