package mihon.video.engine.spi.player

import mihon.video.engine.spi.chapters.FileChapter
import mihon.video.engine.spi.tracks.VideoTracks

/**
 * What a player is playing, as one snapshot. [durationMs] is null while the length is unknown, as before the stream
 * loaded or for a live one; a player that failed keeps the duration it knew. [error] stays set until the player is
 * prepared again.
 */
data class VideoPlayerState(
    val positionMs: Long = 0L,
    val bufferedPositionMs: Long = 0L,
    val durationMs: Long? = null,
    val playback: Playback = Playback.Idle,
    val playWhenReady: Boolean = false,
    val isPlaying: Boolean = false,
    val speed: Float = 1f,
    val volume: Float = 1f,
    /** Null until the first frame's size is known. */
    val videoSize: VideoSize? = null,
    val tracks: VideoTracks = VideoTracks.Empty,
    /** The text subtitles on screen now, which the app draws; subtitles the player draws itself never appear here. */
    val cues: List<SubtitleCue> = emptyList(),
    /** The selected subtitle track is one the player draws itself, which the viewer's appearance doesn't reach. */
    val drawsSelectedSubtitle: Boolean = false,
    /** Empty until the file is loaded, for a file without any, and for a player that doesn't read them. */
    val fileChapters: List<FileChapter> = emptyList(),
    val error: VideoPlayerError? = null,
) {
    enum class Playback {
        /** Not loading: not prepared yet, stopped, or failed. */
        Idle,
        Buffering,
        Ready,
        Ended,
    }
}

/** [pixelAspectRatio] is a pixel's width over its height, 1 for square pixels. */
data class VideoSize(val width: Int, val height: Int, val pixelAspectRatio: Float) {
    /** The picture's width over its height as shown, or null when the size doesn't say. */
    val displayAspectRatio: Float?
        get() {
            if (width <= 0 || height <= 0 || !pixelAspectRatio.isFinite() || pixelAspectRatio <= 0f) return null
            return (width * pixelAspectRatio / height).takeIf { it.isFinite() && it > 0f }
        }
}
