package mihon.translation.api

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class TranslationLanguageTagTest {
    @Test
    fun `language tags are normalized at the API boundary`() {
        TranslationLanguageTag.parse("zh_hant_tw")?.value shouldBe "zh-Hant-TW"
        TranslationLanguageTag.parse("EN-us")?.value shouldBe "en-US"
    }

    @Test
    fun `blank and undetermined language tags are rejected`() {
        TranslationLanguageTag.parse("") shouldBe null
        TranslationLanguageTag.parse("und") shouldBe null
    }
}
