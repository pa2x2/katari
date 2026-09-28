package mihon.entry.interactions.manga.reader.text.session

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

class MangaTextLanguageTest {

    @Test
    fun `only single-language sources declare their content language`() {
        declaredContentLanguage("pt-BR") shouldBe LanguageTag.require("pt-BR")
        listOf("all", "multi", "other", "", null).forEach { code ->
            declaredContentLanguage(code) shouldBe null
        }
    }
}
