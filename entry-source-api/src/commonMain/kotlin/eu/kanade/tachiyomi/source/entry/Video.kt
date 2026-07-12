package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.Serializable

/**
 * A playable video stream option exposed by an anime source.
 */
@Serializable
data class VideoPlaybackOption(
    val key: String,
    val label: String,
    val description: String? = null,
)

/**
 * Request metadata for a video stream or subtitle track.
 */
@Serializable
data class VideoRequest(
    val url: String,
    val headers: Map<String, String> = emptyMap(),
)

/**
 * A playable video stream.
 */
@Serializable
data class VideoStream(
    val request: VideoRequest,
    val label: String = "",
    val type: VideoStreamType = VideoStreamType.UNKNOWN,
    val mimeType: String? = null,
    val key: String = "",
)

@Serializable
enum class VideoStreamType {
    HLS,
    DASH,
    PROGRESSIVE,
    UNKNOWN,
}

/**
 * External subtitle track resolved by a [SubtitleSource].
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
