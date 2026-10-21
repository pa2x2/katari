package mihon.entry.interactions.manga.reader.text.ui

import android.graphics.RectF
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.OverlayPanel
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Captures one drag and reports the outlined rectangle in window coordinates. While shown it takes every touch, so
 * the page underneath does not scroll during the outline; a tap, the back gesture, or the cancel action leaves it.
 */
@Composable
internal fun MangaReaderAreaSelectionLayer(
    onAreaSelected: (RectF) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var origin by remember { mutableStateOf(Offset.Zero) }
    var start by remember { mutableStateOf<Offset?>(null) }
    var end by remember { mutableStateOf<Offset?>(null) }
    val colors = KatariTheme.markup
    val outlineWidth = KatariTheme.sizes.selectedBorder

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            .pointerInput(Unit) { detectTapGestures(onTap = { onCancel() }) }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        start = offset
                        end = offset
                    },
                    onDrag = { change, _ -> end = change.position },
                    onDragEnd = {
                        val outline = outline(start, end)
                        start = null
                        end = null
                        if (outline == null || outline.width < MINIMUM_OUTLINE || outline.height < MINIMUM_OUTLINE) {
                            onCancel()
                        } else {
                            onAreaSelected(
                                RectF(
                                    outline.left + origin.x,
                                    outline.top + origin.y,
                                    outline.right + origin.x,
                                    outline.bottom + origin.y,
                                ),
                            )
                        }
                    },
                    onDragCancel = {
                        start = null
                        end = null
                    },
                )
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(colors.shade)
            outline(start, end)?.let { rect ->
                drawRect(colors.fill, topLeft = rect.topLeft, size = rect.size)
                drawRect(
                    colors.mark,
                    topLeft = rect.topLeft,
                    size = rect.size,
                    style = Stroke(width = outlineWidth.toPx()),
                )
            }
        }
        OverlayPanel(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .safeDrawingPadding()
                .padding(top = KatariTheme.spacing.l),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(MR.strings.reader_text_select_area_hint),
                    modifier = Modifier.padding(
                        start = KatariTheme.spacing.l,
                        top = KatariTheme.spacing.s,
                        bottom = KatariTheme.spacing.s,
                    ),
                    role = TextRole.ItemTitle,
                )
                Button(
                    label = stringResource(MR.strings.action_cancel),
                    onClick = onCancel,
                    style = ButtonStyle.Text,
                )
            }
        }
    }
    BackHandler(onBack = onCancel)
}

private fun outline(start: Offset?, end: Offset?): Rect? {
    if (start == null || end == null) return null
    return Rect(
        left = minOf(start.x, end.x),
        top = minOf(start.y, end.y),
        right = maxOf(start.x, end.x),
        bottom = maxOf(start.y, end.y),
    )
}

/** Pixels below which an outline is treated as an accidental touch. */
private const val MINIMUM_OUTLINE = 24f
