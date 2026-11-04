package eu.kanade.tachiyomi.ui.video.player.subtitle

import mihon.video.engine.spi.player.SubtitleAnchored
import mihon.video.engine.spi.player.SubtitleCue
import mihon.video.engine.spi.player.SubtitleImage
import mihon.video.engine.spi.player.SubtitleLine
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

    data class Text<T : MeasuredSubtitleText>(val text: T, override val left: Float, override val top: Float) :
        PlacedSubtitle<T>

    data class Image(
        val image: SubtitleImage,
        override val left: Float,
        override val top: Float,
        val width: Float,
        val height: Float,
    ) : PlacedSubtitle<Nothing>
}

/**
 * Places [cues] as [layout] says, as Media3's subtitle view placed them, in a drawing [boundsWidth] pixels wide: cues
 * their file places go in the layout's area, kept inside it, and the rest rest on the layout's bottom, each above the
 * one before it. A text cue keeps a padding of an eighth of the text size on either side, and wraps to the area, or to
 * its own width when the file gives one. [measure] lays a cue's text out [width] pixels wide, only as wide as its
 * widest line unless [fixedWidth].
 */
internal fun <T : MeasuredSubtitleText> placeSubtitles(
    cues: List<SubtitleCue>,
    layout: VideoSubtitleLayout,
    boundsWidth: Float,
    measure: (cue: SubtitleCue, width: Int, fixedWidth: Boolean) -> T,
): List<PlacedSubtitle<T>> {
    val area = SubtitleArea(
        left = layout.areaLeft,
        top = layout.areaTop,
        right = minOf(layout.areaLeft + layout.areaWidth, boundsWidth),
        bottom = layout.areaTop + layout.areaHeight,
    )
    if (area.width <= 0f || area.height <= 0f) return emptyList()
    var restingBottom = layout.playerHeight - layout.bottomPx
    return cues.mapNotNull { cue ->
        val image = cue.image
        val placed = if (image != null) {
            placeImage(cue, image, area)
        } else {
            placeText(cue, layout.textSizePx, area, restingBottom, measure)
                ?.also { if (cue.line == null) restingBottom = it.top }
        }
        placed?.shiftedBy(layout.shiftX, layout.shiftY)
    }
}

private class SubtitleArea(val left: Float, val top: Float, val right: Float, val bottom: Float) {
    val width: Float get() = right - left
    val height: Float get() = bottom - top
}

private fun <T : MeasuredSubtitleText> placeText(
    cue: SubtitleCue,
    textSizePx: Float,
    area: SubtitleArea,
    restingBottom: Float,
    measure: (cue: SubtitleCue, width: Int, fixedWidth: Boolean) -> T,
): PlacedSubtitle.Text<T>? {
    if (cue.text.isEmpty()) return null
    val padding = (textSizePx * TEXT_PADDING_SHARE + 0.5f).toInt()
    var available = area.width.toInt() - padding * 2
    cue.width?.let { available = (available * it).toInt() }
    if (available <= 0) return null

    var textWidth = measure(cue, available, false).linesWidth
    if (cue.width != null && textWidth < available) textWidth = available
    textWidth += padding * 2
    val left: Float
    val right: Float
    val position = cue.position
    if (position != null) {
        val anchor = (area.width * position.share).roundToInt() + area.left
        left = maxOf(position.anchor.start(anchor, textWidth.toFloat()), area.left)
        right = minOf(left + textWidth, area.right)
    } else {
        left = (area.width - textWidth) / 2f + area.left
        right = left + textWidth
    }
    if (right - left <= 0f) return null

    val text = measure(cue, (right - left).toInt(), true)
    val height = text.height.toFloat()
    val top = when (val line = cue.line) {
        null -> restingBottom - height
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
    return PlacedSubtitle.Text(text, left, top)
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

private fun <T : MeasuredSubtitleText> PlacedSubtitle<T>.shiftedBy(x: Float, y: Float): PlacedSubtitle<T> =
    when (this) {
        is PlacedSubtitle.Text -> copy(left = left + x, top = top + y)
        is PlacedSubtitle.Image -> copy(left = left + x, top = top + y)
    }

// Media3's subtitle view pads each cue's text by an eighth of the text size on either side
private const val TEXT_PADDING_SHARE = 0.125f
