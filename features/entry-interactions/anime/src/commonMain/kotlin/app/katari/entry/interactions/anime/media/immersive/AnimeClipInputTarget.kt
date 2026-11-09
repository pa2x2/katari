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

    /** Whether the clip has subtitles for [toggleSubtitles] to show or hide. */
    val hasSubtitles: Boolean

    fun toggleSubtitles()

    /** Changes the player's volume by [percent], unmuting the feed, where it has one (`AnimeClipPlatform.playerVolume`). */
    fun stepVolume(percent: Int)

    /** Seeks [stepSeconds] in [direction], with the seek feedback, and hides the controls. */
    fun seek(direction: VideoPlayerSeekDirection, stepSeconds: Int)

    /** Plays at 2× while [boosted]. */
    fun setSpeedBoost(boosted: Boolean)
}
