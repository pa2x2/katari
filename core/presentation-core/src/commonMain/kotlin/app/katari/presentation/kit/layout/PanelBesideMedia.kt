package app.katari.presentation.kit.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.theme.KatariTheme

/**
 * A panel in the room beside media centred in this space, such as the immersive feed's title and actions beside a tall
 * page in a wide window. While the media, [mediaWidth] wide (null while it isn't known), leaves at least 320 dp at its
 * end side, [panel] fills that room to the full height, so it covers none of the picture. [content] is drawn over the
 * whole space and gets the room the panel takes, or null while there is none and its own controls go over the media.
 */
@Composable
fun PanelBesideMedia(
    mediaWidth: Dp?,
    modifier: Modifier = Modifier,
    panel: @Composable () -> Unit,
    content: @Composable (panelRoom: Dp?) -> Unit,
) {
    BoxWithConstraints(modifier = modifier, propagateMinConstraints = true) {
        val room = mediaWidth?.let { (maxWidth - it) / 2 }?.takeIf { it >= MinPanelRoom }
        content(room)
        if (room != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(room)
                    .fillMaxHeight()
                    .windowInsetsPadding(
                        WindowInsets.safeDrawing.only(WindowInsetsSides.Vertical + WindowInsetsSides.End),
                    )
                    .padding(KatariTheme.spacing.s),
            ) {
                panel()
            }
        }
    }
}

// Room for a title over two lines and labelled buttons beside them
private val MinPanelRoom = 320.dp
