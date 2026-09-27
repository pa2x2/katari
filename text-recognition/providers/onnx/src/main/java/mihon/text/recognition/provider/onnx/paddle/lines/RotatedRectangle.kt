package mihon.text.recognition.provider.onnx.paddle.lines

import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * A rectangle of [width] along direction [angle] (radians from the x axis) and [height] across it, centred on
 * ([centerX], [centerY]). Coordinates are continuous: pixel (x, y) covers [x, x + 1) × [y, y + 1).
 */
internal data class RotatedRectangle(
    val centerX: Double,
    val centerY: Double,
    val width: Double,
    val height: Double,
    val angle: Double,
) {
    /** Corners in the order top-left, top-right, bottom-right, bottom-left, as PaddleOCR orders a text box. */
    val corners: List<Point>
        get() {
            val ux = cos(angle) * width / 2
            val uy = sin(angle) * width / 2
            val vx = -sin(angle) * height / 2
            val vy = cos(angle) * height / 2
            val points = listOf(
                Point(centerX - ux - vx, centerY - uy - vy),
                Point(centerX + ux - vx, centerY + uy - vy),
                Point(centerX + ux + vx, centerY + uy + vy),
                Point(centerX - ux + vx, centerY - uy + vy),
            ).sortedBy(Point::x)
            val (topLeft, bottomLeft) = points.take(2).sortedBy(Point::y)
            val (topRight, bottomRight) = points.drop(2).sortedBy(Point::y)
            return listOf(topLeft, topRight, bottomRight, bottomLeft)
        }

    val shortSide: Double
        get() = minOf(width, height)

    /** Whether the rectangle's sides run within a few degrees of the image axes. */
    val isAxisAligned: Boolean
        get() {
            val folded = abs(angle) % (Math.PI / 2)
            return minOf(folded, Math.PI / 2 - folded) < AXIS_TOLERANCE
        }

    fun contains(x: Double, y: Double): Boolean {
        val dx = x - centerX
        val dy = y - centerY
        val along = dx * cos(angle) + dy * sin(angle)
        val across = -dx * sin(angle) + dy * cos(angle)
        return abs(along) <= width / 2 && abs(across) <= height / 2
    }

    /** Grows every side outwards by [distance]. */
    fun expanded(distance: Double) = copy(width = width + 2 * distance, height = height + 2 * distance)

    fun scaled(scaleX: Double, scaleY: Double): Quadrilateral =
        Quadrilateral(corners.map { Point(it.x * scaleX, it.y * scaleY) })

    data class Point(val x: Double, val y: Double)

    /** Four corners in [RotatedRectangle.corners] order, after a transform that may no longer keep right angles. */
    data class Quadrilateral(val corners: List<Point>) {
        /** Longer of the top and bottom edges. */
        val width: Double
            get() = maxOf(corners[0].distanceTo(corners[1]), corners[3].distanceTo(corners[2]))

        /** Longer of the left and right edges. */
        val height: Double
            get() = maxOf(corners[0].distanceTo(corners[3]), corners[1].distanceTo(corners[2]))

        private fun Point.distanceTo(other: Point) = hypot(x - other.x, y - other.y)
    }

    companion object {
        private const val AXIS_TOLERANCE = 0.17 // about 10 degrees

        fun axisAligned(left: Double, top: Double, right: Double, bottom: Double) = RotatedRectangle(
            centerX = (left + right) / 2,
            centerY = (top + bottom) / 2,
            width = right - left,
            height = bottom - top,
            angle = 0.0,
        )
    }
}

/** Axis-aligned bounds of [this] as left, top, right, bottom. */
internal fun RotatedRectangle.bounds(): DoubleArray {
    val corners = corners
    return doubleArrayOf(
        corners.minOf { it.x },
        corners.minOf { it.y },
        corners.maxOf { it.x },
        corners.maxOf { it.y },
    )
}

/**
 * The smallest rectangle enclosing [points], found by rotating calipers over their convex hull: one side of the
 * minimum rectangle always lies on a hull edge.
 */
internal fun minimumAreaRectangle(points: List<RotatedRectangle.Point>): RotatedRectangle {
    val hull = convexHull(points)
    require(hull.size >= 3) { "At least three points that are not collinear are required" }
    var best: RotatedRectangle? = null
    var bestArea = Double.MAX_VALUE
    for (index in hull.indices) {
        val from = hull[index]
        val to = hull[(index + 1) % hull.size]
        val length = hypot(to.x - from.x, to.y - from.y)
        if (length == 0.0) continue
        val ux = (to.x - from.x) / length
        val uy = (to.y - from.y) / length
        var minAlong = Double.MAX_VALUE
        var maxAlong = -Double.MAX_VALUE
        var minAcross = Double.MAX_VALUE
        var maxAcross = -Double.MAX_VALUE
        hull.forEach { point ->
            val along = point.x * ux + point.y * uy
            val across = -point.x * uy + point.y * ux
            minAlong = minOf(minAlong, along)
            maxAlong = maxOf(maxAlong, along)
            minAcross = minOf(minAcross, across)
            maxAcross = maxOf(maxAcross, across)
        }
        val area = (maxAlong - minAlong) * (maxAcross - minAcross)
        if (area < bestArea) {
            bestArea = area
            val along = (minAlong + maxAlong) / 2
            val across = (minAcross + maxAcross) / 2
            best = RotatedRectangle(
                centerX = along * ux - across * uy,
                centerY = along * uy + across * ux,
                width = maxAlong - minAlong,
                height = maxAcross - minAcross,
                angle = atan2(uy, ux),
            )
        }
    }
    return requireNotNull(best).normalized()
}

/** The same rectangle described with its angle in (-45°, 45°], so near-horizontal boxes keep their width along x. */
private fun RotatedRectangle.normalized(): RotatedRectangle {
    var rectangle = this
    val quarter = Math.PI / 2
    while (rectangle.angle > quarter / 2) {
        rectangle =
            rectangle.copy(width = rectangle.height, height = rectangle.width, angle = rectangle.angle - quarter)
    }
    while (rectangle.angle <= -quarter / 2) {
        rectangle =
            rectangle.copy(width = rectangle.height, height = rectangle.width, angle = rectangle.angle + quarter)
    }
    return rectangle
}

/** Andrew's monotone chain; returns the hull counter-clockwise without repeating the first point. */
private fun convexHull(points: List<RotatedRectangle.Point>): List<RotatedRectangle.Point> {
    val sorted = points.distinct().sortedWith(compareBy(RotatedRectangle.Point::x, RotatedRectangle.Point::y))
    if (sorted.size < 3) return sorted
    fun cross(o: RotatedRectangle.Point, a: RotatedRectangle.Point, b: RotatedRectangle.Point) =
        (a.x - o.x) * (b.y - o.y) - (a.y - o.y) * (b.x - o.x)
    val lower = mutableListOf<RotatedRectangle.Point>()
    sorted.forEach { point ->
        while (lower.size >= 2 &&
            cross(lower[lower.size - 2], lower.last(), point) <= 0
        ) {
            lower.removeAt(lower.lastIndex)
        }
        lower += point
    }
    val upper = mutableListOf<RotatedRectangle.Point>()
    sorted.asReversed().forEach { point ->
        while (upper.size >= 2 &&
            cross(upper[upper.size - 2], upper.last(), point) <= 0
        ) {
            upper.removeAt(upper.lastIndex)
        }
        upper += point
    }
    return lower.dropLast(1) + upper.dropLast(1)
}
