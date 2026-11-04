package eu.kanade.tachiyomi.ui.video.player.gesture

import tachiyomi.domain.entry.model.PictureFraming

/**
 * What the viewer's input on the picture reads and drives: Android's touch gestures, and desktop's mouse and keys
 * later. Each event reads it anew, so an implementation answers from the screen's state at that moment rather than
 * from values captured when it was built.
 */
internal interface VideoPlayerInputTarget {
    val subtitleEditorVisible: Boolean
    val controlsLocked: Boolean
    val controlsVisible: Boolean
    val settingsVisible: Boolean

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

    fun previewPictureFraming(framing: PictureFraming)

    fun commitPictureFraming(framing: PictureFraming)
}
