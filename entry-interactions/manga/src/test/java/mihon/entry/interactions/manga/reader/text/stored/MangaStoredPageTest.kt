package mihon.entry.interactions.manga.reader.text.stored

import android.graphics.Bitmap
import io.kotest.matchers.shouldBe
import mihon.entry.interactions.manga.reader.text.geometry.MangaDisplayedPageGeometry
import mihon.entry.interactions.manga.reader.text.geometry.MangaPageTransform
import mihon.entry.interactions.manga.reader.text.image.DisplayedPageImage
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslation
import mihon.entry.interactions.manga.translation.artifact.MangaChapterTranslationSetup
import mihon.entry.interactions.manga.translation.artifact.MangaTranslatedPage
import mihon.entry.interactions.manga.translation.artifact.MangaTranslatedRegion
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.api.image.ImageRect
import mihon.text.recognition.api.image.ImageSize
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.result.RecognizedTextRegion
import mihon.text.recognition.api.result.TextOrientation
import mihon.text.recognition.api.result.TextRegionKind
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.request.ResolvedTranslationRoute
import org.junit.jupiter.api.Test

class MangaStoredPageTest {

    private val bubble = ImageRect(100, 100, 300, 400)
    private val narration = ImageRect(500, 1500, 900, 1600)

    private val translation = MangaChapterTranslation(
        setup = MangaChapterTranslationSetup(
            pageLanguage = LanguageTag.require("ja"),
            pipeline = TextRecognitionPipeline(
                TextRecognitionComponentId("detector"),
                TextRecognitionComponentId("reader"),
            ),
            route = ResolvedTranslationRoute(
                LanguageTag.require("ja"),
                LanguageTag.require("en"),
                TranslationEngineId("engine"),
            ),
        ),
        pages = listOf(
            MangaTranslatedPage(
                fileName = "001.jpg",
                content = ImageContentKey("stored-file"),
                size = ImageSize(1000, 1800),
                regions = listOf(
                    MangaTranslatedRegion(region(bubble, "こんにちは"), translation = "Hello"),
                    MangaTranslatedRegion(region(narration, "ドン"), translation = null),
                ),
            ),
        ),
    )

    @Test
    fun `a page whose file changed since it was translated is not drawn from storage`() {
        translation.pageShownBy(image(rawContent = "re-downloaded-file")) shouldBe null

        // The same file cropped by 20 pixels on each side: regions move with the crop, and only translated ones
        // carry a stored translation.
        val stored = translation.pageShownBy(image(rawContent = "stored-file", shown = ImageRect(20, 20, 980, 1780)))
        stored?.result?.regions?.map { it.bounds } shouldBe
            listOf(ImageRect(80, 80, 280, 380), ImageRect(480, 1480, 880, 1580))
        stored?.translations shouldBe mapOf(ImageRect(80, 80, 280, 380) to "Hello")
    }

    private fun image(rawContent: String, shown: ImageRect = ImageRect(0, 0, 1000, 1800)) =
        object : DisplayedPageImage {
            override val key = ImageContentKey("displayed")
            override val size = ImageSize(shown.width, shown.height)
            override val geometry = MangaDisplayedPageGeometry(MangaPageTransform.None, shown)
            override val rawContent = ImageContentKey(rawContent)
            override suspend fun decodeRegion(region: ImageRect, sampleSize: Int): Bitmap = error("Not decoded")
            override fun close() = Unit
        }

    private fun region(bounds: ImageRect, text: String) = RecognizedTextRegion(
        bounds = bounds,
        text = text,
        kind = TextRegionKind.SpeechBubble,
        orientation = TextOrientation.Vertical,
    )
}
