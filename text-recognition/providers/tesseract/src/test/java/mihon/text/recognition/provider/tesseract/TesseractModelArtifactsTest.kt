package mihon.text.recognition.provider.tesseract

import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

class TesseractModelArtifactsTest {

    @Test
    fun `language data is laid out in the tessdata directory Tesseract loads from`() {
        TesseractModelArtifacts.artifact(TesseractLanguage.Japanese).files.map { it.name } shouldContainExactly
            listOf("tessdata/jpn.traineddata", "tessdata/jpn_vert.traineddata")
    }

    @Test
    fun `regional variants read with their language's data`() {
        TesseractLanguage.forLanguage(LanguageTag.require("pt-BR")) shouldBe TesseractLanguage.Portuguese
        TesseractLanguage.forLanguage(LanguageTag.require("ja-JP")) shouldBe TesseractLanguage.Japanese
    }
}
