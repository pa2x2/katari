package app.katari.desktop.player.pictureinpicture

import java.awt.Rectangle
import kotlin.math.abs
import kotlin.math.roundToInt

/*
 * Where the picture-in-picture window goes and how it is sized, in the units AWT places windows in. Its shape is the
 * video's, so its height always follows from its width.
 */

/** The window's width over its height: the video's, from 2.39:1 wide to 1:2.39 tall, and 16:9 until it is known. */
internal fun pictureInPictureShape(aspectRatio: Float?): Float =
    aspectRatio?.takeIf { it.isFinite() && it > 0f }?.coerceIn(1f / WIDEST_SHAPE, WIDEST_SHAPE) ?: DEFAULT_SHAPE

/** The first time: in the bottom-right corner of [screen], the usable part of the main screen, a quarter as wide. */
internal fun firstPictureInPictureBounds(screen: Rectangle, shape: Float): Rectangle {
    val width = (screen.width * FIRST_SCREEN_SHARE).roundToInt().coerceAtLeast(MIN_WIDTH)
    val height = heightFor(width, shape)
    return Rectangle(
        screen.x + screen.width - width - MARGIN,
        screen.y + screen.height - height - MARGIN,
        width,
        height,
    )
}

/**
 * The window back as [saved], at its width in [shape], on the screen of [screens] it lies on most, or the first, the
 * main one, when it lies on none any more: moved, and shrunk where it is now larger than that screen, to lie on it
 * whole.
 */
internal fun restoredPictureInPictureBounds(saved: Rectangle, shape: Float, screens: List<Rectangle>): Rectangle {
    val screen = screens.maxByOrNull { screen ->
        val shared = saved.intersection(screen)
        if (shared.isEmpty) 0L else shared.width.toLong() * shared.height
    }?.takeUnless { saved.intersection(it).isEmpty } ?: screens.first()
    val width = minOf(saved.width, screen.width, (screen.height * shape).toInt()).coerceAtLeast(MIN_WIDTH)
    val height = heightFor(width, shape)
    return Rectangle(
        saved.x.coerceIn(screen.x, maxOf(screen.x, screen.x + screen.width - width)),
        saved.y.coerceIn(screen.y, maxOf(screen.y, screen.y + screen.height - height)),
        width,
        height,
    )
}

/** An edge or corner of the window that resizes it; [horizontal] and [vertical] are -1 for left and top, 1 for right and bottom. */
internal enum class ResizeEdge(val horizontal: Int, val vertical: Int) {
    Left(-1, 0),
    Right(1, 0),
    Top(0, -1),
    Bottom(0, 1),
    TopLeft(-1, -1),
    TopRight(1, -1),
    BottomLeft(-1, 1),
    BottomRight(1, 1),
}

/**
 * [start] after dragging [edge] by [dx] and [dy], at [shape] and between [MIN_WIDTH] and [maxWidth] wide. A corner
 * follows whichever way the drag went further, and the opposite edge or corner stays where it was; dragging a side
 * keeps the window's top or left where it was.
 */
internal fun resizedPictureInPictureBounds(
    start: Rectangle,
    edge: ResizeEdge,
    dx: Int,
    dy: Int,
    shape: Float,
    maxWidth: Int,
): Rectangle {
    val fromHorizontal = start.width + edge.horizontal * dx
    val fromVertical = ((start.height + edge.vertical * dy) * shape).roundToInt()
    val wanted = when {
        edge.vertical == 0 -> fromHorizontal
        edge.horizontal == 0 -> fromVertical
        abs(fromHorizontal - start.width) >= abs(fromVertical - start.width) -> fromHorizontal
        else -> fromVertical
    }
    val width = wanted.coerceIn(MIN_WIDTH, maxOf(MIN_WIDTH, maxWidth))
    val height = heightFor(width, shape)
    val x = if (edge.horizontal < 0) start.x + start.width - width else start.x
    val y = if (edge.vertical < 0) start.y + start.height - height else start.y
    return Rectangle(x, y, width, height)
}

/** [bounds] at its width in [shape], its top-left corner kept, as when the video's shape becomes known. */
internal fun reshapedPictureInPictureBounds(bounds: Rectangle, shape: Float): Rectangle =
    Rectangle(bounds.x, bounds.y, bounds.width, heightFor(bounds.width, shape))

private fun heightFor(width: Int, shape: Float): Int = (width / shape).roundToInt().coerceAtLeast(1)

/** Small enough to keep out of the way, large enough for its controls. */
internal const val MIN_WIDTH = 288

private const val WIDEST_SHAPE = 2.39f
private const val DEFAULT_SHAPE = 16f / 9f
private const val FIRST_SCREEN_SHARE = 0.25f
private const val MARGIN = 24
