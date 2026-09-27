package mihon.text.recognition.provider.onnx.detector

import io.kotest.matchers.collections.shouldContainExactly
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.spi.component.DetectedTextRegion
import mihon.text.recognition.spi.component.DetectedTextRegionKind
import org.junit.jupiter.api.Test

class ComicTextDetectionsTest {

    @Test
    fun `confident queries become regions of the model classes clipped to the bitmap`() {
        val detections = decodeDetections(
            labels = longArrayOf(0, 1, 2, 1, 7),
            boxes = floatArrayOf(
                366.4f, 1085.2f, 592.9f, 1342.1f,
                -3f, 172f, 336f, 408f,
                24f, 1477f, 145f, 1579f,
                10f, 10f, 20f, 20f,
                0f, 0f, 50f, 50f,
            ),
            scores = floatArrayOf(0.97f, 0.95f, 0.91f, 0.2f, 0.99f),
            width = 1200,
            height = 1800,
        )

        detections shouldContainExactly listOf(
            DetectedTextRegion(ImageRect(366, 1085, 593, 1343), DetectedTextRegionKind.Bubble, 0.97f),
            DetectedTextRegion(ImageRect(0, 172, 336, 408), DetectedTextRegionKind.BubbleText, 0.95f),
            DetectedTextRegion(ImageRect(24, 1477, 145, 1579), DetectedTextRegionKind.FreeText, 0.91f),
        )
    }
}
