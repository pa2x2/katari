package eu.kanade.tachiyomi.ui.video.player.mediacontrols

import mihon.video.engine.spi.player.VideoPlayer

/** The desktop shows no media controls for the player outside the window yet. */
internal object NoMediaControls : VideoPlayerMediaControls {
    override var onNextEpisode: () -> Unit = {}

    override fun attach(player: VideoPlayer, metadata: VideoMetadata, seekStepSeconds: Int, hasNextEpisode: Boolean) =
        Unit
}
