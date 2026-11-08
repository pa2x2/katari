package app.katari.entry.interactions.manga.reader.text.stored

import android.graphics.Bitmap
import app.katari.entry.interactions.manga.reader.text.geometry.MangaDisplayedPageGeometry
import app.katari.entry.interactions.manga.reader.text.image.DisplayedPageImage
import app.katari.entry.interactions.manga.translation.artifact.MangaChapterTranslation
import app.katari.entry.interactions.manga.translation.artifact.MangaChapterTranslationSetup
import app.katari.entry.interactions.manga.translation.artifact.MangaTranslatedPage
import app.katari.entry.interactions.manga.translation.artifact.MangaTranslatedRegion
import app.katari.language.api.tag.LanguageTag
import app.katari.text.recognition.api.component.TextRecognitionComponentId
import app.katari.text.recognition.api.image.ImageContentKey
import app.katari.text.recognition.api.image.ImageRect
import app.katari.text.recognition.api.image.ImageSize
import app.katari.text.recognition.api.pipeline.TextRecognitionPipeline
import app.katari.text.recognition.api.result.RecognizedTextRegion
import app.katari.text.recognition.api.result.TextOrientation
import app.katari.text.recognition.api.result.TextRegionKind
import app.katari.translation.api.engine.TranslationEngineId
import app.katari.translation.api.request.ResolvedTranslationRoute
import io.kotest.matchers.shouldBe
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
            override val geometry = MangaDisplayedPageGeometry(shown)
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
