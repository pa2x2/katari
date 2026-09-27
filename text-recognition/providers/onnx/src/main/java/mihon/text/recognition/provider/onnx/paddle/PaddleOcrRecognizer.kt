package mihon.text.recognition.provider.onnx.paddle

import ai.onnxruntime.OnnxTensor
import android.graphics.Bitmap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.provider.onnx.catalog.OnnxModelArtifacts
import mihon.text.recognition.provider.onnx.catalog.OnnxTextRecognitionCatalog
import mihon.text.recognition.provider.onnx.paddle.lines.RotatedRectangle
import mihon.text.recognition.provider.onnx.paddle.lines.cropLine
import mihon.text.recognition.provider.onnx.paddle.lines.detectTextLines
import mihon.text.recognition.provider.onnx.paddle.lines.linesInReadingOrder
import mihon.text.recognition.provider.onnx.paddle.lines.stackCharacterColumns
import mihon.text.recognition.provider.onnx.session.OnnxSessions
import mihon.text.recognition.provider.onnx.tensor.blue
import mihon.text.recognition.provider.onnx.tensor.green
import mihon.text.recognition.provider.onnx.tensor.planarTensor
import mihon.text.recognition.provider.onnx.tensor.red
import mihon.text.recognition.spi.component.RecognizedCropText
import mihon.text.recognition.spi.component.TextRecognitionComponentAvailability
import mihon.text.recognition.spi.component.TextRecognizer
import mihon.text.recognition.spi.model.TextRecognitionModels
import java.io.File
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * PaddleOCR reads single lines, so a region is first split into lines with the PP-OCRv3 detector, and each line is
 * read with the recognizer for the language's script. Both stages receive their input as PaddleOCR prepares it:
 * in BGR channel order, and for the recognizer padded to its minimum width.
 */
internal class PaddleOcrRecognizer(
    private val sessions: OnnxSessions,
) : TextRecognizer {
    override val catalogEntry = OnnxTextRecognitionCatalog.paddleOcr

    /** Regions are split into lines at up to the line detector's resolution, so crops arrive unreduced. */
    override val inputEdge: Int = LINE_DETECTOR_MAXIMUM_EDGE

    override val processingRevision: Int = 3

    private val dictionaries = mutableMapOf<File, List<String>>()

    override fun models(language: LanguageTag): List<ModelArtifactDescriptor> = listOfNotNull(
        OnnxModelArtifacts.paddleOcrLineDetector,
        PaddleOcrScript.forLanguage(language)?.let(OnnxModelArtifacts::paddleOcrRecognizer),
    )

    override suspend fun inspectDevice(language: LanguageTag) = TextRecognitionComponentAvailability.Available

    override suspend fun recognize(
        crop: Bitmap,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): RecognizedCropText? {
        val script = PaddleOcrScript.forLanguage(language) ?: return null
        val recognizer = models[OnnxModelArtifacts.paddleOcrRecognizer(script)]
        val recognizerFile = recognizer.file(OnnxModelArtifacts.PADDLE_OCR_RECOGNIZER_FILE)
        val dictionary = dictionary(recognizer.file(OnnxModelArtifacts.PADDLE_OCR_DICTIONARY_FILE))
        val source = if (crop.config == Bitmap.Config.HARDWARE) crop.copy(Bitmap.Config.ARGB_8888, false) else crop
        try {
            val lines = detectLines(source, models, script).map { line ->
                // Only scripts printed vertically have tall lines that are columns; elsewhere a tall line is a single
                // tall glyph such as "!", which reads correctly upright.
                ReadableLine(line, vertical = script.vertical && line.height >= line.width * VERTICAL_ASPECT)
            }
            val vertical = lines.count(ReadableLine::vertical) * 2 > lines.size
            val texts = linesInReadingOrder(lines, { it.bounds }, vertical).mapNotNull { line ->
                currentCoroutineContext().ensureActive()
                val bitmap =
                    cropLine(source, line.shape, turnCounterClockwise = line.vertical) ?: return@mapNotNull null
                try {
                    readLine(bitmap, recognizerFile, dictionary).trim().takeIf(String::isNotEmpty)
                } finally {
                    bitmap.recycle()
                }
            }
            if (texts.isEmpty()) return null
            return RecognizedCropText(
                text = texts.joinToString(if (script.spaced) " " else ""),
                orientation = if (vertical) TextOrientation.Vertical else TextOrientation.Horizontal,
            )
        } finally {
            if (source !== crop) source.recycle()
        }
    }

    /** Line shapes in [crop] pixels. */
    private fun detectLines(
        crop: Bitmap,
        models: TextRecognitionModels,
        script: PaddleOcrScript,
    ): List<RotatedRectangle.Quadrilateral> {
        val session = sessions.session(
            slot = LINE_DETECTOR_SLOT,
            file = models[OnnxModelArtifacts.paddleOcrLineDetector].file(
                OnnxModelArtifacts.PADDLE_OCR_LINE_DETECTOR_FILE,
            ),
        )
        val scale = minOf(1f, LINE_DETECTOR_MAXIMUM_EDGE.toFloat() / maxOf(crop.width, crop.height))
        val width = roundToMultiple(crop.width * scale)
        val height = roundToMultiple(crop.height * scale)
        val pixels = planarTensor(crop, width, height) { pixel, channel ->
            when (channel) {
                0 -> (pixel.blue() / 255f - IMAGENET_MEAN[0]) / IMAGENET_STD[0]
                1 -> (pixel.green() / 255f - IMAGENET_MEAN[1]) / IMAGENET_STD[1]
                else -> (pixel.red() / 255f - IMAGENET_MEAN[2]) / IMAGENET_STD[2]
            }
        }
        val shape = longArrayOf(1, 3, height.toLong(), width.toLong())
        val probability = OnnxTensor.createTensor(sessions.environment, pixels, shape).use { input ->
            session.run(mapOf("x" to input)).use { result ->
                val map = (result.get(0) as OnnxTensor).floatBuffer
                FloatArray(map.remaining()).also(map::get)
            }
        }
        val lines = detectTextLines(probability, width, height)
        val scaleX = crop.width.toDouble() / width
        val scaleY = crop.height.toDouble() / height
        return (if (script.vertical) stackCharacterColumns(lines) else lines).map { it.scaled(scaleX, scaleY) }
    }

    private fun readLine(line: Bitmap, recognizerFile: File, dictionary: List<String>): String {
        val session = sessions.session(RECOGNIZER_SLOT, recognizerFile)
        val width = ceil(line.width * RECOGNIZER_HEIGHT.toDouble() / line.height).toInt()
            .coerceIn(1, RECOGNIZER_MAXIMUM_WIDTH)
        val tensorWidth = maxOf(width, RECOGNIZER_MINIMUM_WIDTH)
        val pixels = planarTensor(line, width, RECOGNIZER_HEIGHT, tensorWidth) { pixel, channel ->
            when (channel) {
                0 -> pixel.blue()
                1 -> pixel.green()
                else -> pixel.red()
            } / 127.5f - 1f
        }
        val shape = longArrayOf(1, 3, RECOGNIZER_HEIGHT.toLong(), tensorWidth.toLong())
        return OnnxTensor.createTensor(sessions.environment, pixels, shape).use { input ->
            session.run(mapOf("x" to input)).use { result ->
                val output = result.get(0) as OnnxTensor
                val (_, steps, classes) = output.info.shape
                val scores = output.floatBuffer.let { buffer -> FloatArray(buffer.remaining()).also(buffer::get) }
                decodeCtc(scores, steps.toInt(), classes.toInt(), dictionary)
            }
        }
    }

    @Synchronized
    private fun dictionary(file: File): List<String> = dictionaries.getOrPut(file) { file.readLines() }

    private fun roundToMultiple(value: Float): Int =
        ((value / DETECTOR_STRIDE).roundToInt() * DETECTOR_STRIDE).coerceAtLeast(DETECTOR_STRIDE)

    private class ReadableLine(val shape: RotatedRectangle.Quadrilateral, val vertical: Boolean) {
        val bounds = doubleArrayOf(
            shape.corners.minOf { it.x },
            shape.corners.minOf { it.y },
            shape.corners.maxOf { it.x },
            shape.corners.maxOf { it.y },
        )
    }

    private companion object {
        const val LINE_DETECTOR_SLOT = "paddleocr-line-detector"
        const val RECOGNIZER_SLOT = "paddleocr-recognizer"
        const val LINE_DETECTOR_MAXIMUM_EDGE = 960
        const val DETECTOR_STRIDE = 32
        const val RECOGNIZER_HEIGHT = 48

        /** PaddleOCR pads every line to at least this width, so short lines keep the proportions it was trained on. */
        const val RECOGNIZER_MINIMUM_WIDTH = 320
        const val RECOGNIZER_MAXIMUM_WIDTH = 3200
        const val VERTICAL_ASPECT = 1.5

        /** ImageNet's RGB statistics, which PaddleOCR applies position by position to its BGR channels. */
        val IMAGENET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        val IMAGENET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}
