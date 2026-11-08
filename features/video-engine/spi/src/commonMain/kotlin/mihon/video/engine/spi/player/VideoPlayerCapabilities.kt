package mihon.video.engine.spi.player

/** What a player offers beyond playing: the engines differ in these, and a player keeps them for its whole life. */
data class VideoPlayerCapabilities(
    /** Reads the chapters stored in the file into [VideoPlayerState.fileChapters]. */
    val fileChapters: Boolean = false,
    /** [VideoPlayer.setVolumeBoost] works; false also where the device lacks the audio effect it takes. */
    val volumeBoost: Boolean = false,
    /** Draws some subtitles itself, in their own styling: see [VideoPlayerState.drawsSelectedSubtitle]. */
    val drawsSubtitles: Boolean = false,
    /**
     * Scales and moves the picture inside its video output itself ([VideoPlayer.placePicture]), where otherwise the
     * output's view would be transformed. Its output covers the whole player instead, so what it draws over the
     * picture, such as its own subtitles, stays on screen however far the picture is zoomed.
     */
    val placesPicture: Boolean = false,
    /** [VideoPlayer.stepFrame] moves the paused picture one frame either way. */
    val stepsFrames: Boolean = false,
)
