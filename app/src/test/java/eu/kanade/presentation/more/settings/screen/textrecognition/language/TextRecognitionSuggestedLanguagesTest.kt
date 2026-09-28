package eu.kanade.presentation.more.settings.screen.textrecognition.language

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

class TextRecognitionSuggestedLanguagesTest {

    private val supported = listOf("en", "ja", "ko", "pt", "zh").map(LanguageTag::require)

    @Test
    fun `the device language comes first, then series languages from the most used`() {
        val series = listOf("ko", "ja", "ko", "all", "ko", "ja", "en").map(LanguageTag::require)

        textRecognitionSuggestedLanguages(LanguageTag.require("en-US"), series, supported) shouldBe
            listOf("en", "ko", "ja").map(LanguageTag::require)
    }

    @Test
    fun `regional source languages suggest the language recognition reads`() {
        val series = listOf("pt-BR", "zh-Hant").map(LanguageTag::require)

        textRecognitionSuggestedLanguages(LanguageTag.require("fi"), series, supported) shouldBe
            listOf("pt", "zh").map(LanguageTag::require)
    }
}
