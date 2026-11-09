package app.katari.text.recognition.api.image

/** A [width]×[height] image as unpremultiplied 0xAARRGGBB [values], row by row. */
class ArgbPixels(
    val width: Int,
    val height: Int,
    val values: IntArray,
) {
    init {
        require(width > 0 && height > 0 && values.size == width * height)
    }

    operator fun get(x: Int, y: Int): Int = values[y * width + x]
}
