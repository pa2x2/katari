package eu.kanade.tachiyomi.ui.video.player.pip

import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackSnapshot

/** No picture-in-picture window yet on the desktop, so the controls offer no button for one. */
internal object NoPictureInPicture : VideoPlayerPictureInPicture {
    override val isInPictureInPictureMode: Boolean = false

    override val showsButton: Boolean = false

    override var latestSnapshot = VideoPlayerPlaybackSnapshot()

    override fun onPlaybackSnapshot(snapshot: VideoPlayerPlaybackSnapshot) = Unit

    override fun setEpisodeNavigation(onPreviousEpisode: (() -> Unit)?, onNextEpisode: (() -> Unit)?) = Unit

    override fun enterFromControls() = error("The desktop offers no picture-in-picture button")
}
