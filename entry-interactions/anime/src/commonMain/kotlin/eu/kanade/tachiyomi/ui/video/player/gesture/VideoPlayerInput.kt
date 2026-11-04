package eu.kanade.tachiyomi.ui.video.player.gesture

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mihon.video.engine.spi.player.VideoPlayer

/**
 * How the viewer acts on the watched picture. Android's are the touch gestures on the video's own view; desktop's will
 * be the mouse and keys. The screen draws everything else over the picture.
 */
internal interface VideoPlayerInput {
    /** The watched [player]'s picture, framed as [target] shows it, taking the viewer's input for [target]. */
    @Composable
    fun Picture(player: VideoPlayer, target: VideoPlayerInputTarget, modifier: Modifier)

    /** What the input does, shown on the first visit in a profile; [onDismiss] marks it seen. */
    @Composable
    fun Guide(seekStepSeconds: Int, onDismiss: () -> Unit, modifier: Modifier)
}
