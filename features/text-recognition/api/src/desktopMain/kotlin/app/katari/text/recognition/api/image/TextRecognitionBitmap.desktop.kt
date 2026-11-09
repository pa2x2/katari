package app.katari.text.recognition.api.image

/**
 * Pixels decoded for recognition. Desktop engines read plain pixels, so hosts convert what their decoder produces
 * once, as they decode a region.
 */
actual class TextRecognitionBitmap(
    internal val pixels: ArgbPixels,
)

/** Nothing to free early: the pixels live on the heap. */
actual fun TextRecognitionBitmap.release() = Unit

actual fun TextRecognitionBitmap.readPixels(): ArgbPixels =
    ArgbPixels(pixels.width, pixels.height, pixels.values.copyOf())
