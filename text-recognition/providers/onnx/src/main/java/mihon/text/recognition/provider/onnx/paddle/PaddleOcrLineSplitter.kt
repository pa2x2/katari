package mihon.text.recognition.provider.onnx.paddle

import mihon.text.recognition.api.image.ImageRect

/**
 * Splits a text region into lines from the detector's per-pixel text probability map.
 *
 * Speech bubbles hold short, roughly parallel lines, so lines are the bands of rows (or, for [vertical] text, columns)
 * that contain text. Each line is padded by half its thickness so ascenders and descenders stay inside, and vertical
 * lines are returned right to left.
 */
internal fun splitLines(
    probability: FloatArray,
    width: Int,
    height: Int,
    vertical: Boolean,
    threshold: Float = TEXT_THRESHOLD,
): List<ImageRect> {
    require(probability.size == width * height)
    fun text(x: Int, y: Int) = probability[y * width + x] > threshold
    val across = if (vertical) width else height
    val along = if (vertical) height else width
    fun lineHasText(line: Int) = (0 until along).any { position ->
        if (vertical) text(line, position) else text(position, line)
    }

    val bands = mutableListOf<IntRange>()
    var start = -1
    for (line in 0..across) {
        val hasText = line < across && lineHasText(line)
        if (hasText && start < 0) start = line
        if (!hasText && start >= 0) {
            if (line - start >= MINIMUM_THICKNESS) bands += start until line
            start = -1
        }
    }
    val lines = bands.mapNotNull { band ->
        val occupied = (0 until along).filter { position ->
            band.any { line -> if (vertical) text(line, position) else text(position, line) }
        }
        if (occupied.isEmpty()) return@mapNotNull null
        val padding = (band.last - band.first + 1) / 2
        val acrossStart = (band.first - padding).coerceAtLeast(0)
        val acrossEnd = (band.last + 1 + padding).coerceAtMost(across)
        val alongStart = (occupied.first() - padding).coerceAtLeast(0)
        val alongEnd = (occupied.last() + 1 + padding).coerceAtMost(along)
        if (vertical) {
            ImageRect(acrossStart, alongStart, acrossEnd, alongEnd)
        } else {
            ImageRect(alongStart, acrossStart, alongEnd, acrossEnd)
        }
    }
    return if (vertical) lines.reversed() else lines
}

private const val TEXT_THRESHOLD = 0.3f

/** Bands thinner than this many map pixels are noise, not text lines. */
private const val MINIMUM_THICKNESS = 3
