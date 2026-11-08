package eu.kanade.tachiyomi.ui.video.player.surface

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.video.engine.spi.player.VideoPlayer
import tachiyomi.domain.entry.model.PictureFraming

/** The seek preview's picture, drawn into Compose as the watched one is, which the popup clips and moves with it. */
internal object ComposeSeekPreviewSurface : SeekPreviewSurface {
    @Composable
    override fun Picture(player: VideoPlayer, modifier: Modifier) {
        VideoSurface(player = player, framing = { PictureFraming.Fit }, aspectRatio = { null }, modifier = modifier)
    }
}
