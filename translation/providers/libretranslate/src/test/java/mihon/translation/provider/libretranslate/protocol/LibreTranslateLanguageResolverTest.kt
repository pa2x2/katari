package mihon.translation.provider.libretranslate.protocol

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

class LibreTranslateLanguageResolverTest {

    @Test
    fun `exact tags win and only an unambiguous base language falls back`() {
        val english = language("en")
        val resolver = LibreTranslateLanguageResolver(listOf(english, language("pt-BR"), language("pt-PT")))

        resolver.resolve(LanguageTag.require("en-US")) shouldBe english
        resolver.resolve(LanguageTag.require("pt")) shouldBe null
        resolver.resolve(LanguageTag.require("pt-BR"))?.code shouldBe "pt-BR"
    }

    private fun language(code: String) = LibreTranslateLanguage(code, code, emptySet())
}
