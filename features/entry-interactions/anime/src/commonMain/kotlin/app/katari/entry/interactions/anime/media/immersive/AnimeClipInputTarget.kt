package app.katari.entry.interactions.anime.media.immersive

import eu.kanade.tachiyomi.ui.video.player.gesture.VideoPlayerSeekDirection

/** What the viewer's input does to a clip in the immersive feed. */
internal interface AnimeClipInputTarget {
    /** Whether the clip is the feed's current one, which alone takes keys. */
    val active: Boolean

    /** Whether a seek's feedback still shows, so a tap goes on seeking as the double tap before it did. */
    val seekFeedbackShown: Boolean

    fun toggleControls()

    fun togglePlayback()

    fun toggleMute()

    /** Seeks [stepSeconds] in [direction], with the seek feedback, and hides the controls. */
    fun seek(direction: VideoPlayerSeekDirection, stepSeconds: Int)

    /** Plays at 2× while [boosted]. */
    fun setSpeedBoost(boosted: Boolean)
}
