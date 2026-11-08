package mihon.text.recognition.provider.onnx.paddle

/**
 * A line read by the recognizer. [confidence] is PaddleOCR's line score: the mean probability of the classes that
 * produced characters, or 0 when none did.
 */
internal data class PaddleOcrLineReading(val text: String, val confidence: Float)
