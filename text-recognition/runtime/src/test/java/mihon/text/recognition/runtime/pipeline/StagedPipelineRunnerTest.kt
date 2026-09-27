package mihon.text.recognition.runtime.pipeline

import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.runtime.FakeDetector
import mihon.text.recognition.runtime.FakePageImage
import mihon.text.recognition.runtime.FakeRecognizer
import mihon.text.recognition.runtime.JAPANESE
import mihon.text.recognition.spi.component.DetectedTextRegionKind
import mihon.text.recognition.spi.model.TextRecognitionModels
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
    ) = StagedPipelineRunner(FakeDetector(page, objects), FakeRecognizer(page, texts)).run(
        image = page,
        area = page.size.bounds,
        outlinedByUser = false,
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
    fun `text labelled both inside and outside a bubble is read once`() = runTest {
        val regions = run(
            objects = listOf(
                narration to DetectedTextRegionKind.FreeText,
                narration to DetectedTextRegionKind.BubbleText,
            ),
            texts = mapOf(narration to "語り"),
        )

        regions.map { it.text to it.bounds } shouldContainExactly listOf("語り" to narration)
    }
}
