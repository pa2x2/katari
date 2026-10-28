package mihon.text.recognition.api.image

/**
 * Desktop has no recognition engine yet, so no image region is ever decoded for one; the type gets its pixels once
 * desktop has an engine to read them.
 */
actual class TextRecognitionBitmap private constructor()
