package mihon.text.recognition.provider.onnx.recognizer

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtSession
import android.graphics.Bitmap
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
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
import java.nio.FloatBuffer
import java.nio.LongBuffer

/** Manga OCR vision-encoder/text-decoder, decoded greedily one token at a time. */
internal class MangaOcrRecognizer(
    private val sessions: OnnxSessions,
) : TextRecognizer {
    override val catalogEntry = OnnxTextRecognitionCatalog.mangaOcr
    override fun models(language: LanguageTag): List<ModelArtifactDescriptor> = listOf(OnnxModelArtifacts.mangaOcr)
    override val inputEdge: Int = INPUT_EDGE

    private var vocabulary: Pair<File, MangaOcrVocabulary>? = null

    override suspend fun inspectDevice(language: LanguageTag) = TextRecognitionComponentAvailability.Available

    override suspend fun recognize(
        crop: Bitmap,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): RecognizedCropText? {
        val installed = models[OnnxModelArtifacts.mangaOcr]
        val encoder = sessions.session(ENCODER_SLOT, installed.file(OnnxModelArtifacts.MANGA_OCR_ENCODER_FILE))
        val decoder = sessions.session(DECODER_SLOT, installed.file(OnnxModelArtifacts.MANGA_OCR_DECODER_FILE))
        val vocabulary = vocabulary(installed.file(OnnxModelArtifacts.MANGA_OCR_VOCABULARY_FILE))

        // The model was trained on grayscale crops, normalized to [-1, 1].
        val pixels = planarTensor(crop, INPUT_EDGE, INPUT_EDGE) { pixel, _ ->
            val luminance = (pixel.red() * 299 + pixel.green() * 587 + pixel.blue() * 114) / 1000f
            luminance / 127.5f - 1f
        }
        val shape = longArrayOf(1, 3, INPUT_EDGE.toLong(), INPUT_EDGE.toLong())
        val ids = OnnxTensor.createTensor(sessions.environment, pixels, shape).use { input ->
            encoder.run(mapOf("pixel_values" to input)).use { encoded ->
                decode(decoder, hiddenStates = encoded.get(0) as OnnxTensor)
            }
        }
        return vocabulary.decode(ids).takeIf(String::isNotEmpty)?.let(::RecognizedCropText)
    }

    private suspend fun decode(decoder: OrtSession, hiddenStates: OnnxTensor): List<Int> {
        val ids = mutableListOf(MangaOcrVocabulary.START_TOKEN)
        repeat(MAXIMUM_TOKENS) {
            currentCoroutineContext().ensureActive()
            val next = OnnxTensor.createTensor(
                sessions.environment,
                LongBuffer.wrap(ids.map(Int::toLong).toLongArray()),
                longArrayOf(1, ids.size.toLong()),
            ).use { inputIds ->
                decoder.run(mapOf("input_ids" to inputIds, "encoder_hidden_states" to hiddenStates)).use { result ->
                    val logits = (result.get(0) as OnnxTensor).floatBuffer
                    val vocabularySize = logits.remaining() / ids.size
                    argmax(logits, offset = (ids.size - 1) * vocabularySize, length = vocabularySize)
                }
            }
            if (next == MangaOcrVocabulary.END_TOKEN) return ids.drop(1)
            ids += next
        }
        return ids.drop(1)
    }

    private fun argmax(values: FloatBuffer, offset: Int, length: Int): Int {
        var best = 0
        var bestValue = Float.NEGATIVE_INFINITY
        for (index in 0 until length) {
            val value = values.get(offset + index)
            if (value > bestValue) {
                bestValue = value
                best = index
            }
        }
        return best
    }

    @Synchronized
    private fun vocabulary(file: File): MangaOcrVocabulary {
        vocabulary?.let { (loadedFile, loaded) -> if (loadedFile == file) return loaded }
        return MangaOcrVocabulary.parse(file.readLines().asSequence()).also { vocabulary = file to it }
    }

    private companion object {
        const val INPUT_EDGE = 224
        const val MAXIMUM_TOKENS = 300
        const val ENCODER_SLOT = "manga-ocr-encoder"
        const val DECODER_SLOT = "manga-ocr-decoder"
    }
}
