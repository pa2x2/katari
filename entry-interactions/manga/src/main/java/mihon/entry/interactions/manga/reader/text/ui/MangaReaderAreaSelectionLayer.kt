package mihon.entry.interactions.manga.reader.text.ui

import android.graphics.RectF
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.dp
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Captures one drag and reports the outlined rectangle in window coordinates. While shown it takes every touch, so
 * the page underneath does not scroll during the outline.
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
    val color = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
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
            drawRect(Color.Black.copy(alpha = 0.2f))
            outline(start, end)?.let { rect ->
                drawRect(color.copy(alpha = 0.2f), topLeft = rect.topLeft, size = rect.size)
                drawRect(color, topLeft = rect.topLeft, size = rect.size, style = Stroke(width = 2.dp.toPx()))
            }
        }
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 16.dp),
            shape = RoundedCornerShape(16.dp),
            tonalElevation = 6.dp,
        ) {
            Text(
                text = stringResource(MR.strings.reader_text_select_area_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
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
