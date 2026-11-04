package mihon.video.engine.spi

import androidx.media3.common.MediaMetadata

/** What a player is for, which decides how a [VideoEngine] sets it up. */
sealed interface VideoPlayerRole {
    /**
     * The player the viewer watches. It holds audio focus, pauses when the audio output goes away, keeps the device
     * awake while it plays, and seeks back and forward by [seekStepMs]. [metadata] is what the media notification
     * shows.
     */
    class Watched(
        val metadata: MediaMetadata,
        val seekStepMs: Long,
    ) : VideoPlayerRole

    /** The silent player behind the seek preview: small video frames only, and it never starts playing by itself. */
    data object SeekPreview : VideoPlayerRole

    /** A player in the immersive feed, which plays without taking audio focus. */
    data object Feed : VideoPlayerRole
}
