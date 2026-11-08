package eu.kanade.tachiyomi.ui.video.player.surface

import android.view.TextureView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import app.katari.video.engine.spi.player.TextureViewOutput
import app.katari.video.engine.spi.player.VideoPlayer

// A texture, unlike a surface, is clipped and moved with the popup around it
internal object TextureSeekPreviewSurface : SeekPreviewSurface {
    @Composable
    override fun Picture(player: VideoPlayer, modifier: Modifier) {
        var textureView by remember { mutableStateOf<TextureView?>(null) }
        DisposableEffect(player, textureView) {
            val view = textureView ?: return@DisposableEffect onDispose {}
            player.setVideoOutput(TextureViewOutput(view))
            onDispose { player.setVideoOutput(null) }
        }
        AndroidView(
            modifier = modifier,
            factory = { context -> TextureView(context).also { textureView = it } },
        )
    }
}
