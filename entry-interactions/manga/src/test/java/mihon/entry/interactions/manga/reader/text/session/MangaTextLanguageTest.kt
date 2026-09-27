package mihon.entry.interactions.manga.reader.text.session

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

class MangaTextLanguageTest {

    @Test
    fun `single-language sources declare their content language`() {
        declaredContentLanguage("ja") shouldBe LanguageTag.require("ja")
        declaredContentLanguage("pt-BR") shouldBe LanguageTag.require("pt-BR")
    }

    @Test
    fun `multi-language and unknown sources leave the language to the reader`() {
        listOf("all", "multi", "other", "", null).forEach { code ->
            declaredContentLanguage(code) shouldBe null
        }
    }
}
