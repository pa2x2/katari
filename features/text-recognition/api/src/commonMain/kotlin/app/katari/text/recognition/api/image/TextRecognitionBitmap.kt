package app.katari.text.recognition.api.image

/** Decoded pixels of an image region, in the platform's own bitmap type, which its recognition engines read. */
expect class TextRecognitionBitmap

/** Frees the pixels at once rather than when the bitmap is collected; the bitmap is not used afterwards. */
expect fun TextRecognitionBitmap.release()

/** Every pixel as 0xAARRGGBB, row by row. */
expect fun TextRecognitionBitmap.readArgbPixels(): IntArray
