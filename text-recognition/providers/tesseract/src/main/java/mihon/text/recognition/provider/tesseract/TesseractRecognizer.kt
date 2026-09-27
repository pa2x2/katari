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

/**
 * Reads a detected region with Tesseract. Tall regions of scripts with a vertical model are read as vertical text.
 * Engines are kept per model because initializing one loads its data from disk.
 */
internal class TesseractRecognizer : TextRecognizer {
    override val catalogEntry = TesseractTextRecognitionCatalog.recognizer
    override val inputEdge: Int = INPUT_EDGE

    private val engines = mutableMapOf<File, TessBaseAPI>()

    override fun models(language: LanguageTag): List<ModelArtifactDescriptor> =
        listOfNotNull(TesseractLanguage.forLanguage(language)?.let(TesseractModelArtifacts::artifact))

    override suspend fun inspectDevice(language: LanguageTag) = TextRecognitionComponentAvailability.Available

    override suspend fun recognize(
        crop: Bitmap,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): RecognizedCropText? = read(crop, language, models)

    /** Tesseract engines are not thread-safe, so every read holds this recognizer. */
    @Synchronized
    private fun read(crop: Bitmap, language: LanguageTag, models: TextRecognitionModels): RecognizedCropText? {
        val tesseractLanguage = TesseractLanguage.forLanguage(language) ?: return null
        val installed = models[TesseractModelArtifacts.artifact(tesseractLanguage)]
        val vertical = tesseractLanguage.vertical?.takeIf { crop.height > crop.width * VERTICAL_ASPECT }
        val model = vertical ?: tesseractLanguage.model
        val engine = engine(installed.file(TesseractModelArtifacts.fileName(model)), model.code)
        engine.pageSegMode = if (vertical != null) {
            TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK_VERT_TEXT
        } else {
            TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK
        }
        engine.setImage(crop)
        val text = try {
            engine.utF8Text.orEmpty()
        } finally {
            engine.clear()
        }
        val normalized = if (tesseractLanguage.spaced) {
            text.lines().joinToString(" ") { it.trim() }.replace(WHITESPACE_RUN, " ")
        } else {
            text.filterNot(Char::isWhitespace)
        }.trim()
        return normalized.takeIf(String::isNotEmpty)?.let {
            RecognizedCropText(it, if (vertical != null) TextOrientation.Vertical else TextOrientation.Horizontal)
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

    private companion object {
        /** Tesseract reads text best at around 30 px per line, which crops of this size preserve. */
        const val INPUT_EDGE = 600
        const val VERTICAL_ASPECT = 1.5f
        val WHITESPACE_RUN = Regex("""\s+""")
    }
}
