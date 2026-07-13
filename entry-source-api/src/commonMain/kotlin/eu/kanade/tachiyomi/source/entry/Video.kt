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
 * Request metadata for a video stream or subtitle track.
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
 * @property request URL and headers used by the player.
 * @property label user-visible stream label such as a resolution.
 * @property type transport type used to select playback behavior.
 * @property mimeType optional authoritative media MIME type.
 * @property key stable source-defined stream selection key.
 */
@Serializable
data class VideoStream(
    val request: VideoRequest,
    val label: String = "",
    val type: VideoStreamType = VideoStreamType.UNKNOWN,
    val mimeType: String? = null,
    val key: String = "",
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
 * External subtitle track resolved by a [SubtitleSource].
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
