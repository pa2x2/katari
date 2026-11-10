package eu.kanade.tachiyomi.ui.video.player.subtitle

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import app.katari.video.engine.spi.player.SubtitleAnchored
import app.katari.video.engine.spi.player.SubtitleCue
import app.katari.video.engine.spi.player.SubtitleImage
import app.katari.video.engine.spi.player.SubtitleLine
import app.katari.video.engine.spi.player.SubtitleTextAlignment
import tachiyomi.domain.entry.model.SubtitlePosition
import kotlin.math.roundToInt

/** A cue's text as laid out for drawing, in pixels. */
internal interface MeasuredSubtitleText {
    val width: Int
    val height: Int

    /** The widest line's width. */
    val linesWidth: Int
    val firstLineHeight: Float
}

/** A cue where it is drawn, its top left corner at [left], [top] in pixels of the player. */
internal sealed interface PlacedSubtitle<out T : MeasuredSubtitleText> {
    val left: Float
    val top: Float
    val width: Float
    val height: Float

    data class Text<T : MeasuredSubtitleText>(val text: T, override val left: Float, override val top: Float) :
        PlacedSubtitle<T> {
        override val width: Float get() = text.width.toFloat()
        override val height: Float get() = text.height.toFloat()
    }

    data class Image(
        val image: SubtitleImage,
        override val left: Float,
        override val top: Float,
        override val width: Float,
        override val height: Float,
    ) : PlacedSubtitle<Nothing>
}

/**
 * [cues] where they are drawn, in their order, and [resting], the bounds of the ones the app places, null when the file
 * places every cue.
 */
internal class PlacedSubtitles<out T : MeasuredSubtitleText>(val cues: List<PlacedSubtitle<T>>, val resting: Rect?)

/**
 * Places [cues] as [layout] says, in a drawing [boundsWidth] pixels wide. Cues their file places go in the layout's area
 * as Media3's subtitle view placed them, kept inside it. The rest stack up, each above the one before it, and the stack
 * rests in the layout's cell; their lines lean to the cell's side. A text cue keeps a padding of an eighth of the text
 * size on either side, and wraps to the area, or to its own width when the file gives one. [measure] lays a cue's text
 * out [width] pixels wide, only as wide as its widest line unless [fixedWidth].
 *
 * Every cue is then moved by the layout's shift and held inside the player: the stack as one block, so its lines
 * neither part nor cover each other, and each placed cue alone.
 */
internal fun <T : MeasuredSubtitleText> placeSubtitles(
    cues: List<SubtitleCue>,
    layout: VideoSubtitleLayout,
    boundsWidth: Float,
    measure: (cue: SubtitleCue, width: Int, fixedWidth: Boolean) -> T,
): PlacedSubtitles<T> {
    val area = SubtitleArea(
        left = layout.areaLeft,
        top = layout.areaTop,
        right = minOf(layout.areaLeft + layout.areaWidth, boundsWidth),
        bottom = layout.areaTop + layout.areaHeight,
    )
    if (area.width <= 0f || area.height <= 0f) return PlacedSubtitles(emptyList(), null)
    val player = SubtitleArea(
        left = layout.marginPx,
        top = layout.marginPx,
        right = boundsWidth - layout.marginPx,
        bottom = layout.playerHeight - layout.marginPx,
    )

    // The stack is built upward from 0 and moved to its cell once its height is known
    var stackTop = 0f
    val placed = cues.mapNotNull { cue ->
        val image = cue.image
        val line = cue.line
        when {
            image != null -> placeImage(cue, image, area)?.let(::Placed)
            line != null -> placeText(cue, layout, area, measure)?.let { (text, left) ->
                Placed(PlacedSubtitle.Text(text, left, placedTop(line, text, area)))
            }
            else -> placeText(cue, layout, area, measure)?.let { (text, left) ->
                stackTop -= text.height
                Placed(PlacedSubtitle.Text(text, left, stackTop), resting = true)
            }
        }
    }

    val stack = placed.filter { it.resting }.map { it.subtitle }.bounds()
    val stackShift = stack?.let { bounds ->
        val restingTop = when (layout.position.row) {
            SubtitlePosition.Row.TOP -> layout.restTop
            SubtitlePosition.Row.MIDDLE -> layout.restMiddle - bounds.height / 2f
            SubtitlePosition.Row.BOTTOM -> layout.restBottom - bounds.height
        }
        player.held(bounds.translate(layout.shiftX, layout.shiftY + restingTop - bounds.top)) - bounds.topLeft
    } ?: Offset.Zero
    return PlacedSubtitles(
        cues = placed.map { cue ->
            if (cue.resting) {
                cue.subtitle.movedBy(stackShift)
            } else {
                val shifted = cue.subtitle.movedBy(Offset(layout.shiftX, layout.shiftY))
                val bounds = shifted.bounds()
                shifted.movedBy(player.held(bounds) - bounds.topLeft)
            }
        },
        resting = stack?.translate(stackShift),
    )
}

private class Placed<T : MeasuredSubtitleText>(val subtitle: PlacedSubtitle<T>, val resting: Boolean = false)

private class SubtitleArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top

    /** Where [rect]'s top left goes to move it the least that lays it inside, or its top left when it is too large. */
    fun held(rect: Rect): Offset = Offset(
        x = rect.left + heldBy(rect.left, rect.width, left, right),
        y = rect.top + heldBy(rect.top, rect.height, top, bottom),
    )
}

private fun heldBy(start: Float, size: Float, min: Float, max: Float): Float = when {
    start < min || size > max - min -> min - start
    start + size > max -> max - (start + size)
    else -> 0f
}

/** The text of a cue laid out where it goes across, and its left edge. */
private fun <T : MeasuredSubtitleText> placeText(
    cue: SubtitleCue,
    layout: VideoSubtitleLayout,
    area: SubtitleArea,
    measure: (cue: SubtitleCue, width: Int, fixedWidth: Boolean) -> T,
): Pair<T, Float>? {
    if (cue.text.isEmpty()) return null
    val padding = (layout.textSizePx * TEXT_PADDING_SHARE + 0.5f).toInt()
    var available = area.width.toInt() - padding * 2
    cue.width?.let { available = (available * it).toInt() }
    if (available <= 0) return null

    // A cue the app places leans its lines to the side of its cell
    val column = layout.position.column.takeIf { cue.line == null && cue.position == null }
    val laidOut = when (column) {
        SubtitlePosition.Column.LEFT -> cue.copy(textAlignment = SubtitleTextAlignment.Start)
        SubtitlePosition.Column.RIGHT -> cue.copy(textAlignment = SubtitleTextAlignment.End)
        SubtitlePosition.Column.CENTER, null -> cue
    }
    var textWidth = measure(laidOut, available, false).linesWidth
    if (cue.width != null && textWidth < available) textWidth = available
    textWidth += padding * 2
    val position = cue.position
    val left = when {
        position != null -> maxOf(
            position.anchor.start((area.width * position.share).roundToInt() + area.left, textWidth.toFloat()),
            area.left,
        )
        column == SubtitlePosition.Column.LEFT -> layout.restLeft
        column == SubtitlePosition.Column.RIGHT -> layout.restRight - textWidth
        else -> (area.width - textWidth) / 2f + area.left
    }.coerceAtLeast(area.left)
    val right = minOf(left + textWidth, area.right)
    if (right - left <= 0f) return null
    return measure(laidOut, (right - left).toInt(), true) to left
}

private fun placedTop(line: SubtitleLine, text: MeasuredSubtitleText, area: SubtitleArea): Float {
    val height = text.height.toFloat()
    return when (line) {
        is SubtitleLine.Share -> {
            val anchor = (area.height * line.anchored.share).roundToInt() + area.top
            line.anchored.anchor.start(anchor, height).inside(area, height)
        }
        is SubtitleLine.Row -> {
            val rowTop = if (line.row >= 0f) {
                (line.row * text.firstLineHeight).roundToInt() + area.top
            } else {
                ((line.row + 1f) * text.firstLineHeight).roundToInt() + area.bottom - height
            }
            rowTop.inside(area, height)
        }
    }
}

private fun placeImage(cue: SubtitleCue, image: SubtitleImage, area: SubtitleArea): PlacedSubtitle.Image? {
    val line = (cue.line as? SubtitleLine.Share)?.anchored ?: return null
    val position = cue.position ?: return null
    val widthShare = cue.width ?: return null
    if (image.width <= 0 || image.height <= 0) return null
    val width = (area.width * widthShare).roundToInt().toFloat()
    val height = image.heightShare
        ?.let { (area.height * it).roundToInt().toFloat() }
        ?: (width * image.height / image.width).roundToInt().toFloat()
    return PlacedSubtitle.Image(
        image = image,
        left = position.anchor.start(area.left + area.width * position.share, width).roundToInt().toFloat(),
        top = line.anchor.start(area.top + area.height * line.share, height).roundToInt().toFloat(),
        width = width,
        height = height,
    )
}

/** Where a cue [size] long starts when this side of it is at [anchor]. */
private fun SubtitleAnchored.Anchor.start(anchor: Float, size: Float): Float = when (this) {
    SubtitleAnchored.Anchor.Start -> anchor
    SubtitleAnchored.Anchor.Middle -> anchor - size / 2f
    SubtitleAnchored.Anchor.End -> anchor - size
}

/** This top held so a cue [height] tall stays inside [area]. */
private fun Float.inside(area: SubtitleArea, height: Float): Float = when {
    this + height > area.bottom -> area.bottom - height
    this < area.top -> area.top
    else -> this
}

private fun PlacedSubtitle<*>.bounds(): Rect = Rect(left, top, left + width, top + height)

private fun List<PlacedSubtitle<*>>.bounds(): Rect? =
    map { it.bounds() }.reduceOrNull { all, next -> all.union(next) }

private fun Rect.union(other: Rect): Rect =
    Rect(minOf(left, other.left), minOf(top, other.top), maxOf(right, other.right), maxOf(bottom, other.bottom))

private fun <T : MeasuredSubtitleText> PlacedSubtitle<T>.movedBy(by: Offset): PlacedSubtitle<T> =
    when (this) {
        is PlacedSubtitle.Text -> copy(left = left + by.x, top = top + by.y)
        is PlacedSubtitle.Image -> copy(left = left + by.x, top = top + by.y)
    }

// Media3's subtitle view pads each cue's text by an eighth of the text size on either side
private const val TEXT_PADDING_SHARE = 0.125f
