package eu.kanade.tachiyomi.source.entry

import kotlinx.serialization.Serializable

/**
 * A span of a [VideoStream]'s timeline, shown on the seek bar and, for skippable kinds,
 * offered as a skip button.
 *
 * @property startMs start position in milliseconds.
 * @property endMs end position in milliseconds; must be after [startMs].
 * @property label optional user-visible name, used for [PlaybackMarkerKind.CHAPTER].
 */
@Serializable
data class PlaybackMarker(
    val startMs: Long,
    val endMs: Long,
    val kind: PlaybackMarkerKind,
    val label: String? = null,
)

@Serializable
enum class PlaybackMarkerKind {
    INTRO,

    /** An intro that also carries story, so it is offered for skipping but never skipped automatically. */
    MIXED_INTRO,

    /** End credits. */
    OUTRO,
    RECAP,

    /** A preview of the next episode, usually after the credits. */
    PREVIEW,

    /** A named section that is only shown on the seek bar. */
    CHAPTER,
}
