package mihon.entry.interactions.manga.reader.text.ui.controls

import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import kotlin.math.roundToInt

internal enum class MangaReaderTextControlsEdge { Left, Right }

/**
 * Where translate mode's controls float. Positions are fractions of the room the controls can move in, so they keep
 * their place when the reader's size or its menu changes.
 *
 * @property docked the screen edge the controls are tucked into, or `null` while the toolbar is shown.
 */
internal data class MangaReaderTextControlsPlacement(
    val horizontal: Float = 0.5f,
    val vertical: Float = 1f,
    val docked: MangaReaderTextControlsEdge? = null,
) {
    fun offsetIn(area: IntRect, size: IntSize): IntOffset = IntOffset(
        x = area.left + ((area.width - size.width).coerceAtLeast(0) * horizontal).roundToInt(),
        y = area.top + ((area.height - size.height).coerceAtLeast(0) * vertical).roundToInt(),
    )

    /** The toolbar shown again beside the edge it was tucked into. */
    fun expanded(): MangaReaderTextControlsPlacement = when (docked) {
        null -> this
        MangaReaderTextControlsEdge.Left -> copy(horizontal = 0f, docked = null)
        MangaReaderTextControlsEdge.Right -> copy(horizontal = 1f, docked = null)
    }

    /** Tucked into the side edge the toolbar is nearer to. */
    fun dockedToNearestEdge(): MangaReaderTextControlsPlacement = copy(
        docked = if (horizontal < 0.5f) MangaReaderTextControlsEdge.Left else MangaReaderTextControlsEdge.Right,
    )
}

/**
 * Horizontal positions a dragged toolbar may take: it may be pushed half past either side of [area] to be docked
 * there.
 */
internal fun draggableHorizontalRange(area: IntRect, size: IntSize): IntRange =
    (area.left - size.width / 2)..maxOf(area.left - size.width / 2, area.right - size.width / 2)

/**
 * The placement of a toolbar of [size] dropped at [offset]. A toolbar pushed past a side edge of [area] by more than
 * a quarter of its width is docked to that edge.
 */
internal fun settledPlacement(offset: IntOffset, size: IntSize, area: IntRect): MangaReaderTextControlsPlacement {
    val overhang = size.width * DOCK_OVERHANG
    val vertical = fraction(offset.y - area.top, area.height - size.height)
    return when {
        offset.x < area.left - overhang ->
            MangaReaderTextControlsPlacement(0f, vertical, MangaReaderTextControlsEdge.Left)
        offset.x + size.width > area.right + overhang ->
            MangaReaderTextControlsPlacement(1f, vertical, MangaReaderTextControlsEdge.Right)
        else -> MangaReaderTextControlsPlacement(fraction(offset.x - area.left, area.width - size.width), vertical)
    }
}

/** [placement] with its docked tab of height [tabHeight] moved by [delta] pixels along the edge of [area]. */
internal fun movedAlongEdge(
    placement: MangaReaderTextControlsPlacement,
    delta: Float,
    tabHeight: Int,
    area: IntRect,
): MangaReaderTextControlsPlacement {
    val room = area.height - tabHeight
    if (room <= 0) return placement
    return placement.copy(vertical = (placement.vertical + delta / room).coerceIn(0f, 1f))
}

private fun fraction(position: Int, room: Int): Float =
    if (room <= 0) 0.5f else (position.toFloat() / room).coerceIn(0f, 1f)

private const val DOCK_OVERHANG = 0.25f
