package eu.kanade.tachiyomi.ui.video.player.pip

import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackSnapshot

/**
 * Picture-in-picture: the video alone in a small window that stays over the others, with previous episode, play/pause
 * and next episode controls of its own. Android has the system's; desktop's is a window of its own (D009). The screen
 * hands it the playback state and the episode navigation as they change.
 */
internal interface VideoPlayerPictureInPicture {
    /** Whether the player shows in the small window now; the screen hides its controls and overlays there. */
    val isInPictureInPictureMode: Boolean

    /** Whether the controls offer a button that enters picture-in-picture. */
    val showsButton: Boolean

    /** The playback state the window's controls were last built from. */
    var latestSnapshot: VideoPlayerPlaybackSnapshot

    fun onPlaybackSnapshot(snapshot: VideoPlayerPlaybackSnapshot)

    /** Where the window's previous and next controls go; null greys one out while there's no such episode. */
    fun setEpisodeNavigation(onPreviousEpisode: (() -> Unit)?, onNextEpisode: (() -> Unit)?)

    fun enterFromControls()
}
