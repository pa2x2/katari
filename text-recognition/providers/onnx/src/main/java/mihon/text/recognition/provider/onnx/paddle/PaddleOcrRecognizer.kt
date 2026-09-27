package mihon.text.recognition.provider.onnx.paddle

import ai.onnxruntime.OnnxTensor
import android.graphics.Bitmap
import android.graphics.Matrix
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.provider.onnx.catalog.OnnxModelArtifacts
import mihon.text.recognition.provider.onnx.catalog.OnnxTextRecognitionCatalog
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
import kotlin.math.roundToInt

/**
 * PaddleOCR reads single lines, so a region is first split into lines with the PP-OCRv3 detector, and each line is
 * read with the recognizer for the language's script.
 */
internal class PaddleOcrRecognizer(
    private val sessions: OnnxSessions,
) : TextRecognizer {
    override val catalogEntry = OnnxTextRecognitionCatalog.paddleOcr

    /** Regions are split into lines at up to the line detector's resolution, so crops arrive unreduced. */
    override val inputEdge: Int = LINE_DETECTOR_MAXIMUM_EDGE

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
        val dictionary = dictionary(recognizer.file(OnnxModelArtifacts.PADDLE_OCR_DICTIONARY_FILE))
        val vertical = !script.spaced && crop.height > crop.width * VERTICAL_ASPECT
        val lines = detectLines(crop, models, vertical)
        val texts = lines.mapNotNull { line ->
            currentCoroutineContext().ensureActive()
            readLine(crop, line, vertical, recognizer.file(OnnxModelArtifacts.PADDLE_OCR_RECOGNIZER_FILE), dictionary)
                .trim()
                .takeIf(String::isNotEmpty)
        }
        if (texts.isEmpty()) return null
        return RecognizedCropText(
            text = texts.joinToString(if (script.spaced) " " else ""),
            orientation = if (vertical) TextOrientation.Vertical else TextOrientation.Horizontal,
        )
    }

    /** Line rectangles in [crop] pixels. */
    private fun detectLines(crop: Bitmap, models: TextRecognitionModels, vertical: Boolean): List<ImageRect> {
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
                0 -> (pixel.red() / 255f - IMAGENET_MEAN[0]) / IMAGENET_STD[0]
                1 -> (pixel.green() / 255f - IMAGENET_MEAN[1]) / IMAGENET_STD[1]
                else -> (pixel.blue() / 255f - IMAGENET_MEAN[2]) / IMAGENET_STD[2]
            }
        }
        val shape = longArrayOf(1, 3, height.toLong(), width.toLong())
        val probability = OnnxTensor.createTensor(sessions.environment, pixels, shape).use { input ->
            session.run(mapOf("x" to input)).use { result ->
                val map = (result.get(0) as OnnxTensor).floatBuffer
                FloatArray(map.remaining()).also(map::get)
            }
        }
        val scaleX = crop.width.toFloat() / width
        val scaleY = crop.height.toFloat() / height
        return splitLines(probability, width, height, vertical).map { line ->
            ImageRect(
                left = (line.left * scaleX).toInt().coerceIn(0, crop.width - 1),
                top = (line.top * scaleY).toInt().coerceIn(0, crop.height - 1),
                right = (line.right * scaleX).roundToInt().coerceIn(1, crop.width),
                bottom = (line.bottom * scaleY).roundToInt().coerceIn(1, crop.height),
            )
        }.filter { it.right > it.left && it.bottom > it.top }
    }

    private fun readLine(
        crop: Bitmap,
        line: ImageRect,
        vertical: Boolean,
        recognizerFile: File,
        dictionary: List<String>,
    ): String {
        val session = sessions.session(RECOGNIZER_SLOT, recognizerFile)
        // Vertical lines are turned to read left to right, as PaddleOCR does for tall crops.
        val rotation = Matrix().apply { if (vertical) postRotate(-90f) }
        val lineBitmap = Bitmap.createBitmap(crop, line.left, line.top, line.width, line.height, rotation, true)
        val width = (lineBitmap.width * RECOGNIZER_HEIGHT.toFloat() / lineBitmap.height)
            .roundToInt()
            .coerceIn(RECOGNIZER_MINIMUM_WIDTH, RECOGNIZER_MAXIMUM_WIDTH)
        val pixels = planarTensor(lineBitmap, width, RECOGNIZER_HEIGHT) { pixel, channel ->
            when (channel) {
                0 -> pixel.red()
                1 -> pixel.green()
                else -> pixel.blue()
            } / 127.5f - 1f
        }
        if (lineBitmap !== crop) lineBitmap.recycle()
        val shape = longArrayOf(1, 3, RECOGNIZER_HEIGHT.toLong(), width.toLong())
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

    private companion object {
        const val LINE_DETECTOR_SLOT = "paddleocr-line-detector"
        const val RECOGNIZER_SLOT = "paddleocr-recognizer"
        const val LINE_DETECTOR_MAXIMUM_EDGE = 960
        const val DETECTOR_STRIDE = 32
        const val RECOGNIZER_HEIGHT = 48
        const val RECOGNIZER_MINIMUM_WIDTH = 16
        const val RECOGNIZER_MAXIMUM_WIDTH = 3200
        const val VERTICAL_ASPECT = 1.5f
        val IMAGENET_MEAN = floatArrayOf(0.485f, 0.456f, 0.406f)
        val IMAGENET_STD = floatArrayOf(0.229f, 0.224f, 0.225f)
    }
}
