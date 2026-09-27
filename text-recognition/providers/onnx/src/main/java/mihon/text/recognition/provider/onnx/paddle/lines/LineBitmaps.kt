package mihon.text.recognition.provider.onnx.paddle.lines

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Shader
import kotlin.math.roundToInt

/**
 * Cuts [line] out of [source] as an upright bitmap, as PaddleOCR's perspective crop does. Parts of the line outside
 * [source] repeat its edge pixels.
 *
 * With [turnCounterClockwise], the result is additionally turned a quarter counter-clockwise, which lays a vertical
 * column out the way PaddleOCR presents vertical text to its recognizer.
 */
internal fun cropLine(source: Bitmap, line: RotatedRectangle.Quadrilateral, turnCounterClockwise: Boolean): Bitmap? {
    val width = line.width.roundToInt()
    val height = line.height.roundToInt()
    if (width < 1 || height < 1) return null
    val (topLeft, topRight, bottomRight, bottomLeft) = line.corners
    val from = floatArrayOf(
        topLeft.x.toFloat(),
        topLeft.y.toFloat(),
        topRight.x.toFloat(),
        topRight.y.toFloat(),
        bottomRight.x.toFloat(),
        bottomRight.y.toFloat(),
        bottomLeft.x.toFloat(),
        bottomLeft.y.toFloat(),
    )
    val w = width.toFloat()
    val h = height.toFloat()
    // A quarter turn counter-clockwise sends the top edge to the left and the right edge to the top.
    val to = if (turnCounterClockwise) {
        floatArrayOf(0f, w, 0f, 0f, h, 0f, h, w)
    } else {
        floatArrayOf(0f, 0f, w, 0f, w, h, 0f, h)
    }
    val transform = Matrix().apply { check(setPolyToPoly(from, 0, to, 0, 4)) }
    val shader = BitmapShader(source, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP).apply { setLocalMatrix(transform) }
    val result = if (turnCounterClockwise) {
        Bitmap.createBitmap(height, width, Bitmap.Config.ARGB_8888)
    } else {
        Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    }
    Canvas(result).drawPaint(Paint(Paint.FILTER_BITMAP_FLAG).apply { this.shader = shader })
    return result
}
