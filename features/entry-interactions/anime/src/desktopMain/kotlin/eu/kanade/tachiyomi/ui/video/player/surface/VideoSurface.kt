package eu.kanade.tachiyomi.ui.video.player.surface

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.LocalAwtWindow
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.skiaCanvas
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import eu.kanade.tachiyomi.ui.video.player.picture.placeSelfPlacedPicture
import mihon.presentation.kit.surface.MediaBackdrop
import mihon.video.engine.spi.player.ComposeVideoOutput
import mihon.video.engine.spi.player.VideoPlayer
import org.jetbrains.skia.FilterMipmap
import org.jetbrains.skia.FilterMode
import org.jetbrains.skia.MipmapMode
import org.jetbrains.skia.Rect
import tachiyomi.domain.entry.model.PictureFraming
import java.util.WeakHashMap

/**
 * Draws [player]'s picture here: the picture of a video of [aspectRatio] placed as [framing] asks, on the media colour,
 * under whatever is drawn over it. Both are read outside composition, so a change of framing reaches the picture
 * without composing it again.
 *
 * A player draws into the surface composed for it last, so its picture moves from one window to another, as into
 * picture-in-picture, without a gap, whichever surface leaves the composition first.
 *
 * The player renders whole frames at this drawing's size in pixels, the picture already placed in them (mpv places it
 * itself), into a [ComposeVideoOutput]; each new frame redraws this drawing only. Until the player has a frame, and
 * for a stream with no picture, the media colour shows.
 */
@Composable
internal fun VideoSurface(
    player: VideoPlayer,
    framing: () -> PictureFraming,
    aspectRatio: () -> Float?,
    modifier: Modifier = Modifier,
) {
    val window = LocalAwtWindow.current
    if (window == null) {
        MediaBackdrop(modifier)
        return
    }
    val output = remember(window) { ComposeVideoOutput(window) }
    val frameNumber = output.frameNumber.collectAsState()
    var size by remember { mutableStateOf(IntSize.Zero) }
    val latestFraming by rememberUpdatedState(framing)
    val latestAspectRatio by rememberUpdatedState(aspectRatio)

    DisposableEffect(player, output) {
        drawnInto[player] = output
        player.setVideoOutput(output)
        onDispose {
            // The picture moving into another window is drawn there first, maybe before this leaves
            if (drawnInto[player] === output) {
                drawnInto.remove(player)
                player.setVideoOutput(null)
            }
        }
    }
    LaunchedEffect(player) {
        if (!player.capabilities.placesPicture) return@LaunchedEffect
        snapshotFlow { Triple(size, latestFraming(), latestAspectRatio()) }
            .collect { (size, framing, aspectRatio) ->
                placeSelfPlacedPicture(player, size.width.toFloat(), size.height.toFloat(), framing, aspectRatio)
            }
    }

    MediaBackdrop(
        modifier = modifier
            .onSizeChanged {
                size = it
                output.resize(it.width, it.height)
            }
            .drawWithContent {
                drawContent()
                frameNumber.value
                val frame = output.frame() ?: return@drawWithContent
                drawIntoCanvas { canvas ->
                    canvas.skiaCanvas.drawImageRect(
                        frame,
                        Rect.makeWH(frame.width.toFloat(), frame.height.toFloat()),
                        Rect.makeWH(size.width.toFloat(), size.height.toFloat()),
                        FRAME_SAMPLING,
                        null,
                        true,
                    )
                }
            },
    )
}

// The output each player draws into, on the UI thread, which every window's composition runs on
private val drawnInto = WeakHashMap<VideoPlayer, ComposeVideoOutput>()

// A frame rendered below the drawing's size, as for a toolkit drawing above the screen's scale, is stretched smoothly
private val FRAME_SAMPLING = FilterMipmap(FilterMode.LINEAR, MipmapMode.NONE)
