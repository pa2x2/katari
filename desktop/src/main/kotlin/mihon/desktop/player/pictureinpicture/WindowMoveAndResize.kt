package mihon.desktop.player.pictureinpicture

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.isPrimaryPressed
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.input.pointer.pointerInput
import mihon.presentation.kit.theme.KatariTheme
import java.awt.Cursor
import java.awt.MouseInfo
import java.awt.Point
import java.awt.Window

/*
 * Moving and resizing a window that has neither a title bar nor a frame for the window manager to move and resize it
 * by. The pointer's place on the screen, rather than in the window, drives both, as the window moves under it.
 */

/**
 * Dragging the window by anything in it that leaves the pointer to it moves the window, and a double-click on it
 * calls [onDoubleClick].
 */
internal fun Modifier.movesWindow(window: Window, onDoubleClick: () -> Unit): Modifier = pointerInput(window) {
    var lastClickEndMs = Long.MIN_VALUE
    awaitEachGesture {
        val down = awaitFirstDown()
        if (!currentEvent.buttons.isPrimaryPressed) return@awaitEachGesture
        val pointerStart = pointerOnScreen() ?: return@awaitEachGesture
        val windowStart = window.location
        var moved = false
        var endMs = down.uptimeMillis
        while (true) {
            val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
            endMs = change.uptimeMillis
            if (!change.pressed) break
            if (!moved && (change.position - down.position).getDistance() > viewConfiguration.touchSlop) moved = true
            if (moved) {
                val pointer = pointerOnScreen() ?: continue
                window.setLocation(
                    windowStart.x + pointer.x - pointerStart.x,
                    windowStart.y + pointer.y - pointerStart.y,
                )
                change.consume()
            }
        }
        lastClickEndMs = when {
            moved -> Long.MIN_VALUE
            down.uptimeMillis - lastClickEndMs <= viewConfiguration.doubleTapTimeoutMillis -> {
                onDoubleClick()
                Long.MIN_VALUE
            }
            else -> endMs
        }
    }
}

/**
 * The window's edges and corners, over [BoxScope]'s content, which resize the window as they are dragged, at [shape]
 * (see [resizedPictureInPictureBounds]).
 */
@Composable
internal fun BoxScope.ResizeEdges(window: Window, shape: Float) {
    val edge = KatariTheme.spacing.s
    val corner = edge * 2
    Box(modifier = Modifier.matchParentSize()) {
        Handle(
            window,
            shape,
            ResizeEdge.Top,
            Modifier.align(Alignment.TopCenter).padding(horizontal = corner).fillMaxWidth().height(edge),
        )
        Handle(
            window,
            shape,
            ResizeEdge.Bottom,
            Modifier.align(Alignment.BottomCenter).padding(horizontal = corner).fillMaxWidth().height(edge),
        )
        Handle(
            window,
            shape,
            ResizeEdge.Left,
            Modifier.align(Alignment.CenterStart).padding(vertical = corner).fillMaxHeight().width(edge),
        )
        Handle(
            window,
            shape,
            ResizeEdge.Right,
            Modifier.align(Alignment.CenterEnd).padding(vertical = corner).fillMaxHeight().width(edge),
        )
        Handle(window, shape, ResizeEdge.TopLeft, Modifier.align(Alignment.TopStart).size(corner))
        Handle(window, shape, ResizeEdge.TopRight, Modifier.align(Alignment.TopEnd).size(corner))
        Handle(window, shape, ResizeEdge.BottomLeft, Modifier.align(Alignment.BottomStart).size(corner))
        Handle(window, shape, ResizeEdge.BottomRight, Modifier.align(Alignment.BottomEnd).size(corner))
    }
}

@Composable
private fun Handle(window: Window, shape: Float, edge: ResizeEdge, modifier: Modifier) {
    Box(
        modifier = modifier
            .pointerHoverIcon(PointerIcon(Cursor(edge.cursor)))
            .pointerInput(window, shape, edge) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    down.consume()
                    val pointerStart = pointerOnScreen() ?: return@awaitEachGesture
                    val start = window.bounds
                    val maxWidth = window.graphicsConfiguration.bounds.width
                    while (true) {
                        val change = awaitPointerEvent().changes.firstOrNull { it.id == down.id } ?: break
                        if (!change.pressed) break
                        change.consume()
                        val pointer = pointerOnScreen() ?: continue
                        window.bounds = resizedPictureInPictureBounds(
                            start = start,
                            edge = edge,
                            dx = pointer.x - pointerStart.x,
                            dy = pointer.y - pointerStart.y,
                            shape = shape,
                            maxWidth = maxWidth,
                        )
                    }
                }
            },
    )
}

private val ResizeEdge.cursor: Int
    get() = when (this) {
        ResizeEdge.Left -> Cursor.W_RESIZE_CURSOR
        ResizeEdge.Right -> Cursor.E_RESIZE_CURSOR
        ResizeEdge.Top -> Cursor.N_RESIZE_CURSOR
        ResizeEdge.Bottom -> Cursor.S_RESIZE_CURSOR
        ResizeEdge.TopLeft -> Cursor.NW_RESIZE_CURSOR
        ResizeEdge.TopRight -> Cursor.NE_RESIZE_CURSOR
        ResizeEdge.BottomLeft -> Cursor.SW_RESIZE_CURSOR
        ResizeEdge.BottomRight -> Cursor.SE_RESIZE_CURSOR
    }

// Null where the pointer is on no screen the toolkit knows, as between screens
private fun pointerOnScreen(): Point? = MouseInfo.getPointerInfo()?.location
