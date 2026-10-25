package mihon.video.engine.spi.tracks

import eu.kanade.tachiyomi.source.entry.VideoAudioTrack

/** The key an external audio track is chosen and downloaded by. */
fun audioChoiceKey(track: VideoAudioTrack): String {
    return track.key.ifBlank {
        listOf(track.label, track.language, track.request.url).joinToString("|")
    }
}

/**
 * The external audio track a playing track comes from. An engine publishes the tracks it loads for a stream's audio
 * tracks with a `Format.id` of `<n>:…`, n counting them from 1 in order: the form ExoPlayer's merging media source gives
 * the n-th source merged after the stream, whose own tracks are `0:…`.
 */
fun externalAudioTrackOf(formatId: String?, audioTracks: List<VideoAudioTrack>): VideoAudioTrack? {
    if (audioTracks.isEmpty()) return null
    val position = formatId?.substringBefore(':', missingDelimiterValue = "")?.toIntOrNull() ?: return null
    return audioTracks.getOrNull(position - 1)
}
