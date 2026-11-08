package eu.kanade.tachiyomi.ui.video.player.pip

import androidx.compose.runtime.Composable
import app.katari.entry.interactions.player.AnimePlayerNavigation
import app.katari.entry.interactions.player.AnimePlayerPictureInPictureWindow
import eu.kanade.tachiyomi.ui.video.player.playback.VideoPlayerPlaybackSnapshot

/**
 * Desktop's picture-in-picture (D009): the player goes on in a window of the app's own ([window]), and the screen
 * hands that window the picture, its subtitles and the end screen, while the player itself stays where it was
 * composed. Only the picture moves: the same player and stream play on, and come back to the main window as they are.
 * The screen's own previous and next episode actions serve the window's buttons, through the scene's input.
 */
internal class WindowPictureInPicture(
    private val window: AnimePlayerPictureInPictureWindow,
    private val navigation: () -> AnimePlayerNavigation,
    private val inPictureInPicture: () -> Boolean,
) : VideoPlayerPictureInPicture {
    override val isInPictureInPictureMode: Boolean
        get() = inPictureInPicture()

    override val showsButton = true

    override val drawsOwnWindow = true

    override var latestSnapshot = VideoPlayerPlaybackSnapshot()

    override fun onPlaybackSnapshot(snapshot: VideoPlayerPlaybackSnapshot) {
        latestSnapshot = snapshot
    }

    override fun setEpisodeNavigation(onPreviousEpisode: (() -> Unit)?, onNextEpisode: (() -> Unit)?) = Unit

    override fun enterFromControls() = navigation().enterPictureInPicture()

    @Composable
    override fun Window(scene: VideoPlayerPictureInPictureScene) {
        if (!isInPictureInPictureMode) return
        val returnToPlayer = { navigation().bringToMainWindow() }
        val close = { navigation().close() }
        window.Window(
            title = scene.title,
            aspectRatio = scene.input.videoAspectRatio,
            onDoubleClick = returnToPlayer,
            onCloseRequest = close,
        ) {
            PictureInPictureContent(scene, onReturn = returnToPlayer, onClose = close)
        }
    }
}
