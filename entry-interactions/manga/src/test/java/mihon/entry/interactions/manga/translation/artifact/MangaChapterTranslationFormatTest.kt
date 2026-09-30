package mihon.entry.interactions.manga.translation.artifact

import io.kotest.matchers.shouldBe
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

class MangaChapterTranslationFormatTest {

    @Test
    fun `a version 1 file from an earlier release still reads, and a later version is not misread`() {
        val stored = """
            {"format":1,
             "setup":{"pageLanguage":"ja","detector":"comic.detector","recognizer":"manga.ocr",
                      "engine":"android-system","source":"ja","target":"en"},
             "pages":[{"file":"001.jpg","sha256":"9f86d081884c7d65","width":1200,"height":1800,
                       "regions":[{"bounds":[100,120,300,500],"container":[80,100,320,520],"kind":"bubble",
                                   "orientation":"vertical","text":"こんにちは","translation":"Hello"},
                                  {"bounds":[700,1400,1100,1500],"kind":"free","orientation":"horizontal",
                                   "text":"ドン"}]}]}
        """.trimIndent()

        MangaChapterTranslationFormat.decode(stored) shouldBe MangaChapterTranslation(
            setup = MangaChapterTranslationSetup(
                pageLanguage = LanguageTag.require("ja"),
                pipeline = TextRecognitionPipeline(
                    detector = TextRecognitionComponentId("comic.detector"),
                    recognizer = TextRecognitionComponentId("manga.ocr"),
                ),
                route = ResolvedTranslationRoute(
                    sourceLanguage = LanguageTag.require("ja"),
                    targetLanguage = LanguageTag.require("en"),
                    engine = TranslationEngineId("android-system"),
                ),
            ),
            pages = listOf(
                MangaTranslatedPage(
                    fileName = "001.jpg",
                    content = ImageContentKey("9f86d081884c7d65"),
                    size = ImageSize(1200, 1800),
                    regions = listOf(
                        MangaTranslatedRegion(
                            RecognizedTextRegion(
                                bounds = ImageRect(100, 120, 300, 500),
                                text = "こんにちは",
                                kind = TextRegionKind.SpeechBubble,
                                orientation = TextOrientation.Vertical,
                                container = ImageRect(80, 100, 320, 520),
                            ),
                            translation = "Hello",
                        ),
                        MangaTranslatedRegion(
                            RecognizedTextRegion(
                                bounds = ImageRect(700, 1400, 1100, 1500),
                                text = "ドン",
                                kind = TextRegionKind.FreeText,
                                orientation = TextOrientation.Horizontal,
                            ),
                            translation = null,
                        ),
                    ),
                ),
            ),
        )
        MangaChapterTranslationFormat.decode(stored.replace("\"format\":1", "\"format\":2")) shouldBe null
    }
}
