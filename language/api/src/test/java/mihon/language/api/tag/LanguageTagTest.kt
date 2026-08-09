package mihon.language.api.tag

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class LanguageTagTest {
    @Test
    fun `language tags are normalized at the API boundary`() {
        LanguageTag.parse("zh_hant_tw")?.value shouldBe "zh-Hant-TW"
        LanguageTag.parse("EN-us")?.value shouldBe "en-US"
    }

    @Test
    fun `blank and undetermined language tags are rejected`() {
        LanguageTag.parse("") shouldBe null
        LanguageTag.parse("und") shouldBe null
    }
}
