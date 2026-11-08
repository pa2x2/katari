package app.katari.text.recognition.runtime.pipeline

import app.katari.text.recognition.api.image.ImageRect
import app.katari.text.recognition.api.image.ImageSize
import app.katari.text.recognition.api.result.TextRegionKind
import app.katari.text.recognition.runtime.FakeDetector
import app.katari.text.recognition.runtime.FakePageImage
import app.katari.text.recognition.runtime.FakeRecognizer
import app.katari.text.recognition.runtime.JAPANESE
import app.katari.text.recognition.runtime.cache.TextDetectionCache
import app.katari.text.recognition.spi.component.DetectedTextRegionKind
import app.katari.text.recognition.spi.model.TextRecognitionModels
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StagedPipelineRunnerTest {

    private val page = FakePageImage(ImageSize(1200, 1800))
    private val bubble = ImageRect(100, 100, 500, 600)
    private val upperPiece = ImageRect(150, 150, 450, 300)
    private val lowerPiece = ImageRect(150, 350, 450, 550)
    private val wholeBubbleText = ImageRect(150, 150, 450, 550)
    private val narration = ImageRect(700, 1400, 1100, 1500)

    private suspend fun run(
        objects: List<Pair<ImageRect, DetectedTextRegionKind>>,
        texts: Map<ImageRect, String>,
        outline: ImageRect? = null,
    ) = StagedPipelineRunner(
        detector = FakeDetector(page, objects),
        recognizer = FakeRecognizer(page, texts),
        detection = PageTextDetection(TextDetectionCache(maximumPages = 1)),
    ).run(
        image = page,
        area = outline ?: page.size.bounds,
        outlinedByUser = outline != null,
        language = JAPANESE,
        models = TextRecognitionModels(emptyList()),
    )

    @Test
    fun `a bubble whose text was detected in pieces is read once as a whole`() = runTest {
        val regions = run(
            objects = listOf(
                bubble to DetectedTextRegionKind.Bubble,
                upperPiece to DetectedTextRegionKind.BubbleText,
                lowerPiece to DetectedTextRegionKind.BubbleText,
            ),
            texts = mapOf(wholeBubbleText to "全体"),
        )

        regions.map { Triple(it.text, it.bounds, it.container) } shouldContainExactly
            listOf(Triple("全体", wholeBubbleText, bubble))
    }

    @Test
    fun `an outline reads the text it mostly covers, as the detector found it on the whole page`() = runTest {
        // The outline cuts off the top of the bubble's text and misses the narration.
        val outline = ImageRect(120, 250, 480, 580)

        val regions = run(
            objects = listOf(
                bubble to DetectedTextRegionKind.Bubble,
                wholeBubbleText to DetectedTextRegionKind.BubbleText,
                narration to DetectedTextRegionKind.FreeText,
            ),
            texts = mapOf(wholeBubbleText to "吹き出し", narration to "語り"),
            outline = outline,
        )

        regions.map { Triple(it.text, it.bounds, it.kind) } shouldContainExactly
            listOf(Triple("吹き出し", wholeBubbleText, TextRegionKind.SpeechBubble))
        page.decoded.first().region shouldBe page.size.bounds
    }
}
