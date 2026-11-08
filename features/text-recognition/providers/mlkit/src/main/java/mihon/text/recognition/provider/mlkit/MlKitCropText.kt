package mihon.text.recognition.provider.mlkit

import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.spi.component.RecognizedCropText

/** A line ML Kit read, with its bounds in crop pixels. */
internal data class MlKitLine(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
)

/**
 * Joins the lines ML Kit read in one crop. Lines that are mostly taller than wide are vertical text, read in columns
 * from right to left; other lines are read top to bottom as ML Kit orders them.
 */
internal fun assembleCropText(lines: List<MlKitLine>, spaced: Boolean): RecognizedCropText? {
    val readable = lines.filter { it.text.isNotBlank() }
    if (readable.isEmpty()) return null
    val tall = readable.count { (it.bottom - it.top) > (it.right - it.left) * VERTICAL_ASPECT }
    val vertical = tall * 2 > readable.size
    val ordered = if (vertical) readable.sortedByDescending { it.left + it.right } else readable
    val text = ordered.joinToString(if (spaced) " " else "") { it.text.trim() }.trim()
    return text.takeIf(String::isNotEmpty)?.let {
        RecognizedCropText(it, if (vertical) TextOrientation.Vertical else TextOrientation.Horizontal)
    }
}

private const val VERTICAL_ASPECT = 1.5f
