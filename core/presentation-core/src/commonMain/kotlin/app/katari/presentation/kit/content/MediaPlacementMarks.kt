package app.katari.presentation.kit.content

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import app.katari.presentation.kit.surface.Surface
import app.katari.presentation.kit.surface.SurfaceContainer
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import kotlin.math.roundToInt

/** A line over media that something being moved can line up with, in pixels of the media. */
@Immutable
data class PlacementGuide(
    /** Down the media [at] pixels from its left edge, rather than across it [at] pixels from its top. */
    val vertical: Boolean,
    val at: Float,
    /** Where the line starts and ends along its length. */
    val from: Float,
    val to: Float,
    val label: String,
    /** What is moved is lined up with it, which draws the line stronger and names it. */
    val reached: Boolean,
)

/**
 * Marks over media for something the person moves on it, such as subtitles in the subtitle editor: [frame], in pixels
 * of this element, outlined dashed while it can be moved and solid while [moving]; the [guides] it lines up with; and a
 * [hint] beside the frame saying how to move it. It takes no input, so what is under it keeps every touch.
 */
@Composable
fun MediaPlacementMarks(
    frame: Rect?,
    moving: Boolean,
    guides: List<PlacementGuide>,
    hint: String?,
    modifier: Modifier = Modifier,
) {
    val mark = KatariTheme.markup.mark
    val shade = KatariTheme.markup.shade
    val onMedia = KatariTheme.colors.onMedia
    val density = LocalDensity.current
    val line = with(density) { KatariTheme.sizes.selectedBorder.toPx() }
    val dash = with(density) { DASH.toPx() }
    val gap = with(density) { KatariTheme.spacing.s.toPx() }
    val outline = frame?.inflate(gap)

    Box(modifier) {
        Canvas(Modifier.matchParentSize()) {
            guides.forEach { guide ->
                val (start, end) = if (guide.vertical) {
                    Offset(guide.at, guide.from) to Offset(guide.at, guide.to)
                } else {
                    Offset(guide.from, guide.at) to Offset(guide.to, guide.at)
                }
                drawLine(
                    color = if (guide.reached) mark else mark.copy(alpha = GUIDE_ALPHA),
                    start = start,
                    end = end,
                    strokeWidth = if (guide.reached) line else line / 2f,
                )
            }
            if (outline != null) {
                // A shade under the outline keeps it seen over a bright picture
                drawRect(shade, outline.topLeft, outline.size, style = Stroke(line * 2f))
                val pattern = if (moving) null else PathEffect.dashPathEffect(floatArrayOf(dash, dash))
                drawRect(
                    color = if (moving) mark else onMedia,
                    topLeft = outline.topLeft,
                    size = outline.size,
                    style = Stroke(line, pathEffect = pattern),
                )
            }
        }
        guides.filter(PlacementGuide::reached).forEach { guide ->
            MarkLabel(guide.label, SurfaceContainer.Primary) { width, height ->
                if (guide.vertical) {
                    Offset(guide.at + gap, guide.from + gap)
                } else {
                    Offset(guide.from + gap, guide.at - height - gap)
                }
            }
        }
        if (hint != null && outline != null) {
            MarkLabel(hint, SurfaceContainer.MediaScrim) { width, height ->
                // Above the frame, or under it where the frame is too near the top
                val above = outline.top - gap - height
                Offset(outline.center.x - width / 2f, if (above >= 0f) above else outline.bottom + gap)
            }
        }
    }
}

/** A pill whose top left corner [at] puts, given its measured width and height. */
@Composable
private fun MarkLabel(text: String, container: SurfaceContainer, at: (width: Float, height: Float) -> Offset) {
    Surface(
        modifier = Modifier.layout { measurable, constraints ->
            val placeable = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
            layout(placeable.width, placeable.height) {
                val corner = at(placeable.width.toFloat(), placeable.height.toFloat())
                placeable.place(corner.x.roundToInt(), corner.y.roundToInt())
            }
        },
        container = container,
        shape = KatariTheme.shapes.full,
    ) {
        Text(
            text = text,
            role = TextRole.Badge,
            modifier = Modifier.padding(horizontal = KatariTheme.spacing.m, vertical = KatariTheme.spacing.xs),
        )
    }
}

private val DASH = 6.dp

// Guides not reached yet show where they are without competing with the picture
private const val GUIDE_ALPHA = 0.5f
