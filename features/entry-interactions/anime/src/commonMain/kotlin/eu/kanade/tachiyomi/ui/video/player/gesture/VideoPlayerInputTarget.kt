package eu.kanade.tachiyomi.ui.video.player.gesture

import tachiyomi.domain.entry.model.PictureFraming

/**
 * What the viewer's input on the picture reads and drives: Android's touch gestures, and desktop's mouse and keys,
 * which also reach the commands after the picture's own. Each event reads it anew, so an implementation answers from
 * the screen's state at that moment rather than from values captured when it was built.
 */
internal interface VideoPlayerInputTarget {
    val subtitleEditorVisible: Boolean
    val controlsLocked: Boolean
    val controlsVisible: Boolean
    val settingsVisible: Boolean

    /** The settings panel, the episode list or the subtitle editor is open over the picture, taking the keys. */
    val panelOpen: Boolean

    /** True while a seek gesture's feedback is up, when single taps on either side seek again. */
    val seekGestureModeActive: Boolean
    val speedBoostEligible: Boolean
    val speedBoostActive: Boolean

    /** Set when a double tap has seeked, so the release of that tap doesn't seek a second time. */
    var ignoreNextSeekTapUp: Boolean

    fun setSpeedBoostActive(active: Boolean)

    fun hideControls()

    fun registerControllerInteraction(showControls: Boolean)

    fun performGestureSeek(direction: VideoPlayerSeekDirection)

    fun sideGestureLevel(type: VideoPlayerSideGestureType): Int

    fun sideGestureMaxLevel(type: VideoPlayerSideGestureType): Int

    fun applySideGestureLevel(type: VideoPlayerSideGestureType, level: Int)

    /** Starts a sideways seek from the playing position; false when it can't, such as while the length is unknown. */
    fun startSeekSwipe(): Boolean

    /** Moves the sideways seek's target [deltaMs] from where it started. */
    fun seekSwipeBy(deltaMs: Long)

    /** Ends the sideways seek, seeking to its target when [commit], or staying put. */
    fun finishSeekSwipe(commit: Boolean)

    /** The video's shape, null until it has one. */
    val videoAspectRatio: Float?

    /** The framing on screen, a pinch's preview included. */
    val pictureFraming: PictureFraming

    /** Shows [framing] while a gesture sets it, hiding the controls, which would cover the picture being framed. */
    fun previewPictureFraming(framing: PictureFraming)

    fun commitPictureFraming(framing: PictureFraming)

    fun togglePlayback()

    /** The seek step setting, which [seekStep] seeks by. */
    val seekStepSeconds: Int

    /** Seeks one seek step, showing the seek chevrons and leaving the controls as they are. */
    fun seekStep(direction: VideoPlayerSeekDirection)

    /** Whether the window has a full screen, as desktop's has. */
    val hasFullScreen: Boolean

    fun toggleFullScreen()

    /** Whether the player has a picture-in-picture window to move into from the keys, as desktop's has. */
    val hasPictureInPicture: Boolean

    fun enterPictureInPicture()

    /** Opens the settings panel at its start. */
    fun openSettings()

    fun openEpisodes()

    /** Mutes or unmutes the player's own volume, where the controls set it. */
    fun toggleMute()

    /** Moves to the next speed the settings offer, faster or slower. */
    fun stepSpeed(faster: Boolean)

    /** True while paused with a player that steps frames. */
    val stepsFrames: Boolean

    fun stepFrame(forward: Boolean)

    val hasPreviousEpisode: Boolean
    val hasNextEpisode: Boolean

    fun previousEpisode()

    fun nextEpisode()

    /** The stream has subtitles to turn on and off. */
    val hasSubtitles: Boolean

    /** Turns subtitles off, or back on to the track last turned off, or else the first track. */
    fun toggleSubtitles()

    /** Skip shows for an intro, recap, credits or preview. */
    val skipOffered: Boolean

    fun skip()
}
