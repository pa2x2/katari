package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.Serializable

/**
 * A playable video stream option exposed by an anime source.
 *
 * @property key stable source-defined selection key.
 * @property label user-visible option label.
 * @property description optional user-visible explanation.
 */
@Serializable
data class VideoPlaybackOption(
    val key: String,
    val label: String,
    val description: String? = null,
)

/**
 * Request metadata for a video stream, subtitle or audio track.
 *
 * @property url absolute media URL.
 * @property headers request headers required by the media host.
 */
@Serializable
data class VideoRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

/**
 * A playable video stream.
 *
 * @property request URL and headers used by the player, or `null` when the source is a
 * [DeferredStreamSource] that resolves it on demand.
 * @property label user-visible stream label such as a resolution. When blank, the host
 * builds one from [height] and [bitrate].
 * @property type transport type used to select playback behavior.
 * @property mimeType optional authoritative media MIME type.
 * @property key stable source-defined stream selection key.
 * @property height vertical resolution in pixels, used to match the user's preferred quality.
 * @property bitrate average bitrate in bits per second.
 * @property subtitles external subtitle tracks for this stream.
 * @property audioTracks external audio tracks played alongside this stream. Leave empty when
 * the stream carries its own audio.
 * @property markers intro, credits and other spans of this stream's timeline.
 * @property mpvOptions mpv options this stream needs, by option name without the leading dashes,
 * such as `demuxer-lavf-o`. They apply to this stream only. A stream with any plays in mpv when
 * the user lets the player choose and the mpv add-on is installed; the built-in player ignores
 * them.
 */
@Serializable
data class VideoStream(
    val request: VideoRequest?,
    val label: String = "",
    val type: VideoStreamType = VideoStreamType.UNKNOWN,
    val mimeType: String? = null,
    val key: String = "",
    val height: Int? = null,
    val bitrate: Long? = null,
    val subtitles: List<VideoSubtitle> = emptyList(),
    val audioTracks: List<VideoAudioTrack> = emptyList(),
    val markers: List<PlaybackMarker> = emptyList(),
    val mpvOptions: Map<String, String> = emptyMap(),
)

/** Transport type of a [VideoStream]. */
@Serializable
enum class VideoStreamType {
    HLS,
    DASH,
    PROGRESSIVE,
    UNKNOWN,
}

/**
 * External subtitle track of a [VideoStream].
 *
 * @property request URL and headers used to load the subtitle.
 * @property label user-visible track label.
 * @property language optional BCP 47 language tag.
 * @property mimeType optional authoritative subtitle MIME type.
 * @property key stable source-defined track key.
 * @property isDefault whether the provider marks this track as default.
 * @property isForced whether the provider marks this as a forced-subtitle track.
 */
@Serializable
data class VideoSubtitle(
    val request: VideoRequest,
    val label: String,
    val language: String? = null,
    val mimeType: String? = null,
    val key: String = "",
    val isDefault: Boolean = false,
    val isForced: Boolean = false,
)

/**
 * External audio track of a [VideoStream].
 *
 * @property request URL and headers used to load the audio.
 * @property label user-visible track label.
 * @property language optional BCP 47 language tag.
 * @property mimeType optional authoritative audio MIME type.
 * @property type transport type of the audio media.
 * @property key stable source-defined track key.
 * @property isDefault whether the provider marks this track as default.
 */
@Serializable
data class VideoAudioTrack(
    val request: VideoRequest,
    val label: String,
    val language: String? = null,
    val mimeType: String? = null,
    val type: VideoStreamType = VideoStreamType.UNKNOWN,
    val key: String = "",
    val isDefault: Boolean = false,
)
