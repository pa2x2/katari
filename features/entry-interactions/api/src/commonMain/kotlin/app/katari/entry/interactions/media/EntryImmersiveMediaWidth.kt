package app.katari.entry.interactions.media

import androidx.compose.ui.unit.IntSize

/**
 * How wide media of [aspectRatio] (width over height) is drawn when fitted whole into [container], in pixels; null
 * while either isn't known.
 */
fun fittedMediaWidth(container: IntSize, aspectRatio: Float?): Int? {
    if (aspectRatio == null || aspectRatio <= 0f || container.width <= 0 || container.height <= 0) return null
    return minOf(container.width.toFloat(), container.height * aspectRatio).toInt()
}
