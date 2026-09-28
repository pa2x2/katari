package mihon.text.recognition.provider.onnx.paddle

/**
 * Greedy CTC decoding of a PaddleOCR recognizer output of [steps] × [classes] probabilities.
 *
 * Class 0 is the CTC blank, classes 1..n are the dictionary characters in file order, and the class after the last
 * character is a space. Repeated classes collapse unless separated by a blank.
 */
internal fun decodeCtc(scores: FloatArray, steps: Int, classes: Int, dictionary: List<String>): PaddleOcrLineReading {
    require(scores.size == steps * classes)
    require(classes == dictionary.size + 2) { "Recognizer classes do not match its dictionary" }
    val text = StringBuilder()
    var confidenceSum = 0f
    var characters = 0
    var previous = BLANK
    for (step in 0 until steps) {
        var best = 0
        for (candidate in 1 until classes) {
            if (scores[step * classes + candidate] > scores[step * classes + best]) best = candidate
        }
        if (best != previous && best != BLANK) {
            text.append(if (best == classes - 1) " " else dictionary[best - 1])
            confidenceSum += scores[step * classes + best]
            characters++
        }
        previous = best
    }
    return PaddleOcrLineReading(text.toString(), if (characters == 0) 0f else confidenceSum / characters)
}

private const val BLANK = 0
