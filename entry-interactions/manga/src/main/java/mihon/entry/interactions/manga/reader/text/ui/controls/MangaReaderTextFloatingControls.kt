package mihon.entry.interactions.manga.reader.text.ui.controls

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import mihon.entry.interactions.manga.reader.text.session.MangaReaderTextProgress
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt

/**
 * Floats translate mode's toolbar over the page. The toolbar can be dragged anywhere clear of the reader's menus;
 * pushed past a side edge, it is tucked into that edge as a tab so it stays out of the way while reading.
 */
@Composable
internal fun MangaReaderTextFloatingControls(
    placement: MangaReaderTextControlsPlacement,
    onPlacementChange: (MangaReaderTextControlsPlacement) -> Unit,
    menuVisible: Boolean,
    progress: MangaReaderTextProgress,
    toolbar: @Composable (Modifier) -> Unit,
) {
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val insets = WindowInsets.safeDrawing
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val area = with(density) {
            val margin = MARGIN.roundToPx()
            IntRect(
                left = insets.getLeft(this, layoutDirection) + margin,
                top = insets.getTop(this) + (if (menuVisible) TOP_MENU_CLEARANCE else MARGIN).roundToPx(),
                right = constraints.maxWidth - insets.getRight(this, layoutDirection) - margin,
                bottom = constraints.maxHeight - insets.getBottom(this) -
                    (if (menuVisible) BOTTOM_MENU_CLEARANCE else MARGIN).roundToPx(),
            )
        }
        val currentPlacement by rememberUpdatedState(placement)
        val currentArea by rememberUpdatedState(area)
        val currentOnPlacementChange by rememberUpdatedState(onPlacementChange)
        val docked = placement.docked
        if (docked == null) {
            var size by remember { mutableStateOf(IntSize.Zero) }
            var dragged by remember { mutableStateOf<IntOffset?>(null) }
            val dockLabel = stringResource(MR.strings.reader_text_controls_dock)
            toolbar(
                Modifier
                    .layout { measurable, constraints ->
                        // Measured against the area it is placed in, so a toolbar that fills its width stays on screen.
                        val placeable = measurable.measure(
                            constraints.copy(
                                minWidth = 0,
                                minHeight = 0,
                                maxWidth = area.width.coerceIn(0, constraints.maxWidth),
                            ),
                        )
                        size = IntSize(placeable.width, placeable.height)
                        layout(placeable.width, placeable.height) {
                            placeable.place(dragged ?: placement.offsetIn(area, size))
                        }
                    }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = { dragged = currentPlacement.offsetIn(currentArea, size) },
                            onDrag = { change, amount ->
                                change.consume()
                                dragged = dragged?.moved(amount, currentArea, size)
                            },
                            onDragEnd = {
                                dragged?.let { currentOnPlacementChange(settledPlacement(it, size, currentArea)) }
                                dragged = null
                            },
                            onDragCancel = { dragged = null },
                        )
                    }
                    .semantics {
                        customActions = listOf(
                            CustomAccessibilityAction(dockLabel) {
                                currentOnPlacementChange(currentPlacement.dockedToNearestEdge())
                                true
                            },
                        )
                    },
            )
        } else {
            var tabHeight by remember { mutableStateOf(0) }
            MangaReaderTextEdgeTab(
                edge = docked,
                progress = progress,
                onExpand = { onPlacementChange(placement.expanded()) },
                modifier = Modifier
                    .layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                        tabHeight = placeable.height
                        layout(placeable.width, placeable.height) {
                            val y = placement.offsetIn(area, IntSize(placeable.width, placeable.height)).y
                            val x = when (docked) {
                                MangaReaderTextControlsEdge.Left -> 0
                                MangaReaderTextControlsEdge.Right -> constraints.maxWidth - placeable.width
                            }
                            placeable.place(x, y)
                        }
                    }
                    .pointerInput(Unit) {
                        detectVerticalDragGestures { change, amount ->
                            change.consume()
                            currentOnPlacementChange(movedAlongEdge(currentPlacement, amount, tabHeight, currentArea))
                        }
                    },
            )
        }
    }
}

private fun IntOffset.moved(amount: Offset, area: IntRect, size: IntSize): IntOffset {
    val vertical = area.top..maxOf(area.top, area.bottom - size.height)
    return IntOffset(
        x = (x + amount.x.roundToInt()).coerceIn(draggableHorizontalRange(area, size)),
        y = (y + amount.y.roundToInt()).coerceIn(vertical),
    )
}

private val MARGIN = 16.dp

/** Room the reader's top bar takes while its menu is shown. */
private val TOP_MENU_CLEARANCE = 72.dp

/** Room the reader's bottom menu takes while it is shown. */
private val BOTTOM_MENU_CLEARANCE = 136.dp
