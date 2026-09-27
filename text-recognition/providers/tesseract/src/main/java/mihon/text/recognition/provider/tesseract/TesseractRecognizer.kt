package mihon.text.recognition.provider.tesseract

import android.graphics.Bitmap
import com.googlecode.tesseract.android.TessBaseAPI
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.spi.component.RecognizedCropText
import mihon.text.recognition.spi.component.TextRecognitionComponentAvailability
import mihon.text.recognition.spi.component.TextRecognizer
import mihon.text.recognition.spi.model.TextRecognitionModels
import java.io.File
import kotlin.math.roundToInt

/**
 * Reads a detected region with Tesseract. Regions of scripts with a vertical model are read both ways, and the
 * reading Tesseract is more confident in wins: a region's shape does not tell whether it holds columns or lines.
 * Engines are kept per model because initializing one loads its data from disk.
 */
internal class TesseractRecognizer : TextRecognizer {
    override val catalogEntry = TesseractTextRecognitionCatalog.recognizer
    override val inputEdge: Int = INPUT_EDGE
    override val processingRevision: Int = 2

    private val engines = mutableMapOf<File, TessBaseAPI>()

    override fun models(language: LanguageTag): List<ModelArtifactDescriptor> =
        listOfNotNull(TesseractLanguage.forLanguage(language)?.let(TesseractModelArtifacts::artifact))

    override suspend fun inspectDevice(language: LanguageTag) = TextRecognitionComponentAvailability.Available

    override suspend fun recognize(
        crop: Bitmap,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): RecognizedCropText? {
        val tesseractLanguage = TesseractLanguage.forLanguage(language) ?: return null
        val installed = models[TesseractModelArtifacts.artifact(tesseractLanguage)]
        val input = enlarged(crop)
        try {
            fun readWith(model: TesseractModelFile, vertical: Boolean) =
                read(input, installed.file(TesseractModelArtifacts.fileName(model)), model.code, vertical)
            val best = listOfNotNull(
                readWith(tesseractLanguage.model, vertical = false),
                tesseractLanguage.vertical?.let { readWith(it, vertical = true) },
            ).maxBy(Reading::confidence)
            val normalized = if (tesseractLanguage.spaced) {
                best.text.lines().joinToString(" ") { it.trim() }.replace(WHITESPACE_RUN, " ")
            } else {
                best.text.filterNot(Char::isWhitespace)
            }.trim()
            return normalized.takeIf(String::isNotEmpty)?.let {
                RecognizedCropText(it, if (best.vertical) TextOrientation.Vertical else TextOrientation.Horizontal)
            }
        } finally {
            if (input !== crop) input.recycle()
        }
    }

    /** Tesseract engines are not thread-safe, so every read holds this recognizer. */
    @Synchronized
    private fun read(image: Bitmap, modelFile: File, code: String, vertical: Boolean): Reading {
        val engine = engine(modelFile, code)
        engine.pageSegMode = if (vertical) {
            TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT
        } else {
            TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
        }
        engine.setImage(image)
        return try {
            Reading(engine.utF8Text.orEmpty(), engine.meanConfidence(), vertical)
        } finally {
            engine.clear()
        }
    }

    private fun engine(modelFile: File, code: String): TessBaseAPI = engines.getOrPut(modelFile) {
        // Tesseract loads `<data path>/tessdata/<code>.traineddata`.
        val dataPath =
            requireNotNull(modelFile.parentFile?.parentFile) { "Tesseract data is not in a tessdata directory" }
        TessBaseAPI().apply {
            check(init(dataPath.absolutePath, code)) { "Tesseract could not load $code" }
        }
    }

    /**
     * Bubble lettering is small for Tesseract, whose line finding and binarization work better on larger glyphs, so
     * crops are enlarged unless they are already large.
     */
    private fun enlarged(crop: Bitmap): Bitmap {
        val scale = (MAXIMUM_ENLARGED_EDGE.toFloat() / maxOf(crop.width, crop.height)).coerceIn(1f, ENLARGEMENT)
        if (scale <= 1f) return crop
        return Bitmap.createScaledBitmap(
            crop,
            (crop.width * scale).roundToInt(),
            (crop.height * scale).roundToInt(),
            true,
        )
    }

    private class Reading(val text: String, val confidence: Int, val vertical: Boolean)

    private companion object {
        /** Tesseract reads text best at around 30 px per line, which crops of this size preserve. */
        const val INPUT_EDGE = 600
        const val ENLARGEMENT = 2f
        const val MAXIMUM_ENLARGED_EDGE = 2400
        val WHITESPACE_RUN = Regex("""\s+""")
    }
}
