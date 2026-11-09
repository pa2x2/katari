package app.katari.text.recognition.provider.onnx.detector

import ai.onnxruntime.OnnxTensor
import app.katari.language.api.tag.LanguageTag
import app.katari.model.artifacts.api.descriptor.ModelArtifactDescriptor
import app.katari.text.recognition.api.image.TextRecognitionBitmap
import app.katari.text.recognition.api.image.readPixels
import app.katari.text.recognition.provider.onnx.catalog.OnnxModelArtifacts
import app.katari.text.recognition.provider.onnx.catalog.OnnxTextRecognitionCatalog
import app.katari.text.recognition.provider.onnx.session.OnnxSessions
import app.katari.text.recognition.provider.onnx.tensor.blue
import app.katari.text.recognition.provider.onnx.tensor.green
import app.katari.text.recognition.provider.onnx.tensor.planarTensor
import app.katari.text.recognition.provider.onnx.tensor.red
import app.katari.text.recognition.spi.component.DetectedTextRegion
import app.katari.text.recognition.spi.component.TextDetector
import app.katari.text.recognition.spi.component.TextRecognitionComponentAvailability
import app.katari.text.recognition.spi.model.TextRecognitionModels
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
    override val processingRevision: Int = 3

    override suspend fun inspectDevice(language: LanguageTag) = TextRecognitionComponentAvailability.Available

    override suspend fun detect(tile: TextRecognitionBitmap, models: TextRecognitionModels): List<DetectedTextRegion> {
        val file = models[OnnxModelArtifacts.comicTextDetector].file(OnnxModelArtifacts.DETECTOR_FILE)
        val environment = sessions.environment
        val image = tile.readPixels()
        val pixels = planarTensor(image, INPUT_EDGE, INPUT_EDGE) { pixel, channel ->
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
                LongBuffer.wrap(longArrayOf(image.width.toLong(), image.height.toLong())),
                longArrayOf(1, 2),
            ).use { sizes ->
                sessions.lease(SESSION_SLOT, file).use { detector ->
                    detector.session.run(mapOf("images" to images, "orig_target_sizes" to sizes)).use { result ->
                        val labels = (result.get("labels").get() as OnnxTensor).longBuffer
                        val boxes = (result.get("boxes").get() as OnnxTensor).floatBuffer
                        val scores = (result.get("scores").get() as OnnxTensor).floatBuffer
                        return decodeDetections(
                            labels = LongArray(labels.remaining()).also(labels::get),
                            boxes = FloatArray(boxes.remaining()).also(boxes::get),
                            scores = FloatArray(scores.remaining()).also(scores::get),
                            width = image.width,
                            height = image.height,
                        )
                    }
                }
            }
        }
    }

    private companion object {
        const val INPUT_EDGE = 640
        const val SESSION_SLOT = "comic-text-detector"
    }
}
