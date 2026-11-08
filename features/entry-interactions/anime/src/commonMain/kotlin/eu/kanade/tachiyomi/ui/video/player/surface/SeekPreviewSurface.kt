package eu.kanade.tachiyomi.ui.video.player.surface

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.video.engine.spi.player.VideoPlayer

/**
 * Where the seek preview's player draws: a small picture that is clipped and moved with the popup around it, as the
 * platform's engines can draw one.
 */
internal interface SeekPreviewSurface {
    @Composable
    fun Picture(player: VideoPlayer, modifier: Modifier)
}
