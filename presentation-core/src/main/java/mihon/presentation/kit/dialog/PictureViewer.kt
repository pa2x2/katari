package mihon.presentation.kit.dialog

import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Close
import mihon.presentation.kit.button.IconButton
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarActions
import mihon.presentation.kit.navigation.Scaffold
import mihon.presentation.kit.surface.OverlayPill
import mihon.presentation.kit.surface.SurfaceContainer
import mihon.presentation.kit.theme.KatariTheme
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * A picture full screen, such as an illustration in a book. Pinching or double-tapping zooms it, and dragging it down
 * at its own size closes it, as does Close in the bar below, beside the picture's [actions] such as Share and Save.
 */
@Composable
fun PictureViewer(
    picture: ImageBitmap,
    contentDescription: String?,
    onDismissRequest: () -> Unit,
    actions: List<AppBar.Action>,
    snackbarHostState: SnackbarHostState? = null,
) {
    FullScreenDialog(onDismissRequest = onDismissRequest) {
        Scaffold(
            snackbarHostState = snackbarHostState,
            container = SurfaceContainer.None,
            bottomBar = {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(KatariTheme.spacing.xs)
                        .navigationBarsPadding(),
                ) {
                    OverlayPill {
                        IconButton(
                            icon = MaterialSymbols.Rounded.Close,
                            contentDescription = stringResource(MR.strings.action_close),
                            onClick = onDismissRequest,
                        )
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    if (actions.isNotEmpty()) {
                        OverlayPill { Row { AppBarActions(actions) } }
                    }
                }
            },
        ) { contentPadding ->
            ZoomablePicture(
                picture = picture,
                contentDescription = contentDescription,
                onDismissRequest = onDismissRequest,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}

@Composable
private fun ZoomablePicture(
    picture: ImageBitmap,
    contentDescription: String?,
    onDismissRequest: () -> Unit,
    modifier: Modifier,
) {
    var size by remember { mutableStateOf(IntSize.Zero) }
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var dismissDrag by remember { mutableFloatStateOf(0f) }
    val dismissDistance = with(LocalDensity.current) { DismissDragDistance.toPx() }

    // Keeps the zoomed picture covering the viewport instead of letting it slide off an edge.
    fun bounded(target: Offset, targetScale: Float): Offset {
        val maxX = size.width * (targetScale - 1) / 2
        val maxY = size.height * (targetScale - 1) / 2
        return Offset(target.x.coerceIn(-maxX, maxX), target.y.coerceIn(-maxY, maxY))
    }

    // Zooms around the fingers: the point under the centroid stays under it as the scale changes.
    val transform = rememberTransformableState { centroid, zoomChange, panChange, _ ->
        val targetScale = (scale * zoomChange).coerceIn(1f, MAX_SCALE)
        val fromCenter = centroid - Offset(size.width / 2f, size.height / 2f)
        val zoomed = offset + (fromCenter - offset) * (1 - targetScale / scale)
        scale = targetScale
        offset = bounded(zoomed + panChange, targetScale)
    }
    Image(
        bitmap = picture,
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = { tap ->
                        if (scale > 1f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val center = Offset(size.width / 2f, size.height / 2f)
                            scale = DOUBLE_TAP_SCALE
                            offset = bounded((center - tap) * (DOUBLE_TAP_SCALE - 1), DOUBLE_TAP_SCALE)
                        }
                    },
                )
            }
            // Panning belongs to the zoomed picture; at its own size a vertical drag closes the viewer instead.
            .transformable(transform, canPan = { scale > 1f })
            .draggable(
                state = rememberDraggableState { delta -> dismissDrag = (dismissDrag + delta).coerceAtLeast(0f) },
                orientation = Orientation.Vertical,
                enabled = scale == 1f,
                onDragStopped = {
                    if (dismissDrag > dismissDistance) onDismissRequest() else dismissDrag = 0f
                },
            )
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                translationX = offset.x
                translationY = offset.y + dismissDrag
                alpha = 1f - (dismissDrag / (dismissDistance * 2)).coerceIn(0f, DISMISS_FADE)
            },
    )
}

private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

// A drag down shorter than this springs back; the picture fades by at most half while dragged.
private val DismissDragDistance = 120.dp
private const val DISMISS_FADE = 0.5f
