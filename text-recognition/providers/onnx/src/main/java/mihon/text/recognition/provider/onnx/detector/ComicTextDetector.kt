package mihon.text.recognition.provider.onnx.detector

import ai.onnxruntime.OnnxTensor
import android.graphics.Bitmap
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.provider.onnx.catalog.OnnxModelArtifacts
import mihon.text.recognition.provider.onnx.catalog.OnnxTextRecognitionCatalog
import mihon.text.recognition.provider.onnx.session.OnnxSessions
import mihon.text.recognition.provider.onnx.tensor.blue
import mihon.text.recognition.provider.onnx.tensor.green
import mihon.text.recognition.provider.onnx.tensor.planarTensor
import mihon.text.recognition.provider.onnx.tensor.red
import mihon.text.recognition.spi.component.DetectedTextRegion
import mihon.text.recognition.spi.component.TextDetector
import mihon.text.recognition.spi.component.TextRecognitionComponentAvailability
import mihon.text.recognition.spi.model.TextRecognitionModels
import java.nio.LongBuffer

/**
 * RT-DETR detector for speech bubbles and text. The exported model includes its post-processing: it receives the
 * original size and returns labelled boxes in original pixels.
 */
internal class ComicTextDetector(
    private val sessions: OnnxSessions,
) : TextDetector {
    override val catalogEntry = OnnxTextRecognitionCatalog.comicTextDetector
    override fun models(language: LanguageTag): List<ModelArtifactDescriptor> =
        listOf(OnnxModelArtifacts.comicTextDetector)
    override val inputEdge: Int = INPUT_EDGE
    override val processingRevision: Int = 2

    override suspend fun inspectDevice(language: LanguageTag) = TextRecognitionComponentAvailability.Available

    override suspend fun detect(tile: Bitmap, models: TextRecognitionModels): List<DetectedTextRegion> {
        val session = sessions.session(
            slot = SESSION_SLOT,
            file = models[OnnxModelArtifacts.comicTextDetector].file(OnnxModelArtifacts.DETECTOR_FILE),
        )
        val environment = sessions.environment
        val pixels = planarTensor(tile, INPUT_EDGE, INPUT_EDGE) { pixel, channel ->
            when (channel) {
                0 -> pixel.red()
                1 -> pixel.green()
                else -> pixel.blue()
            } / 255f
        }
        val shape = longArrayOf(1, 3, INPUT_EDGE.toLong(), INPUT_EDGE.toLong())
        OnnxTensor.createTensor(environment, pixels, shape).use { images ->
            OnnxTensor.createTensor(
                environment,
                LongBuffer.wrap(longArrayOf(tile.width.toLong(), tile.height.toLong())),
                longArrayOf(1, 2),
            ).use { sizes ->
                session.run(mapOf("images" to images, "orig_target_sizes" to sizes)).use { result ->
                    val labels = (result.get("labels").get() as OnnxTensor).longBuffer
                    val boxes = (result.get("boxes").get() as OnnxTensor).floatBuffer
                    val scores = (result.get("scores").get() as OnnxTensor).floatBuffer
                    return decodeDetections(
                        labels = LongArray(labels.remaining()).also(labels::get),
                        boxes = FloatArray(boxes.remaining()).also(boxes::get),
                        scores = FloatArray(scores.remaining()).also(scores::get),
                        width = tile.width,
                        height = tile.height,
                    )
                }
            }
        }
    }

    private companion object {
        const val INPUT_EDGE = 640
        const val SESSION_SLOT = "comic-text-detector"
    }
}
