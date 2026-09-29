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

/** Manga OCR vision-encoder/text-decoder, decoded with the beam search its model configuration prescribes. */
internal class MangaOcrRecognizer(
    private val sessions: OnnxSessions,
) : TextRecognizer {
    override val catalogEntry = OnnxTextRecognitionCatalog.mangaOcr
    override fun models(language: LanguageTag): List<ModelArtifactDescriptor> = listOf(OnnxModelArtifacts.mangaOcr)
    override val inputEdge: Int = INPUT_EDGE

    override val processingRevision: Int = 3

    private val search = MangaOcrBeamSearch(MangaOcrVocabulary.START_TOKEN, MangaOcrVocabulary.END_TOKEN)
    private var vocabulary: Pair<File, MangaOcrVocabulary>? = null

    override suspend fun inspectDevice(language: LanguageTag) = TextRecognitionComponentAvailability.Available

    override suspend fun recognize(
        crop: Bitmap,
        language: LanguageTag,
        models: TextRecognitionModels,
    ): RecognizedCropText? {
        val installed = models[OnnxModelArtifacts.mangaOcr]
        val vocabulary = vocabulary(installed.file(OnnxModelArtifacts.MANGA_OCR_VOCABULARY_FILE))

        // The model was trained on grayscale crops stretched to a square and normalized to [-1, 1].
        val pixels = planarTensor(crop, INPUT_EDGE, INPUT_EDGE) { pixel, _ ->
            val luminance = (pixel.red() * 299 + pixel.green() * 587 + pixel.blue() * 114) / 1000f
            luminance / 127.5f - 1f
        }
        val shape = longArrayOf(1, 3, INPUT_EDGE.toLong(), INPUT_EDGE.toLong())
        val encoderFile = installed.file(OnnxModelArtifacts.MANGA_OCR_ENCODER_FILE)
        val decoderFile = installed.file(OnnxModelArtifacts.MANGA_OCR_DECODER_FILE)
        val ids = sessions.lease(ENCODER_SLOT, encoderFile).use { encoder ->
            sessions.lease(DECODER_SLOT, decoderFile).use { decoder ->
                OnnxTensor.createTensor(sessions.environment, pixels, shape).use { input ->
                    encoder.session.run(mapOf("pixel_values" to input)).use { encoded ->
                        val hiddenStates = encoded.get(0) as OnnxTensor
                        decode(decoder.session, hiddenStates.floatBuffer.copy(), hiddenStates.info.shape)
                    }
                }
            }
        }
        return vocabulary.decode(ids).takeIf(String::isNotEmpty)?.let(::RecognizedCropText)
    }

    private suspend fun decode(decoder: OrtSession, hiddenStates: FloatArray, hiddenShape: LongArray): IntArray {
        // Every running beam attends to the same image, so the encoder output is repeated once per beam.
        val repeated = mutableMapOf<Int, OnnxTensor>()
        try {
            return search.search { sequences ->
                currentCoroutineContext().ensureActive()
                val batch = sequences.size
                val length = sequences.first().size
                val states = repeated.getOrPut(batch) {
                    val buffer = FloatBuffer.allocate(hiddenStates.size * batch)
                    repeat(batch) { buffer.put(hiddenStates) }
                    buffer.rewind()
                    OnnxTensor.createTensor(
                        sessions.environment,
                        buffer,
                        longArrayOf(batch.toLong(), hiddenShape[1], hiddenShape[2]),
                    )
                }
                val inputIds = LongBuffer.wrap(
                    LongArray(batch * length) {
                        sequences[it / length][it % length].toLong()
                    },
                )
                OnnxTensor.createTensor(sessions.environment, inputIds, longArrayOf(batch.toLong(), length.toLong()))
                    .use { ids ->
                        decoder.run(mapOf("input_ids" to ids, "encoder_hidden_states" to states)).use { result ->
                            val logits = (result.get(0) as OnnxTensor).floatBuffer
                            val vocabularySize = logits.remaining() / (batch * length)
                            List(batch) { index ->
                                val offset = (index * length + length - 1) * vocabularySize
                                FloatArray(vocabularySize) { logits.get(offset + it) }
                            }
                        }
                    }
            }
        } finally {
            repeated.values.forEach(OnnxTensor::close)
        }
    }

    @Synchronized
    private fun vocabulary(file: File): MangaOcrVocabulary {
        vocabulary?.let { (loadedFile, loaded) -> if (loadedFile == file) return loaded }
        return MangaOcrVocabulary.parse(file.readLines().asSequence()).also { vocabulary = file to it }
    }

    private companion object {
        const val INPUT_EDGE = 224
        const val ENCODER_SLOT = "manga-ocr-encoder"
        const val DECODER_SLOT = "manga-ocr-decoder"

        fun FloatBuffer.copy(): FloatArray = FloatArray(remaining()).also(::get)
    }
}
