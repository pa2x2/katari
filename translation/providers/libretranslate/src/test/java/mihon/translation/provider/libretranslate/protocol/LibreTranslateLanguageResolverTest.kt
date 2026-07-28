package mihon.translation.provider.libretranslate.protocol

import io.kotest.matchers.shouldBe
import mihon.translation.api.TranslationLanguageTag
import org.junit.jupiter.api.Test

class LibreTranslateLanguageResolverTest {

    @Test
    fun `exact tags win and unique base language fallback is allowed`() {
        val english = language("en", targets = setOf("fr"))
        val french = language("fr", targets = setOf("en"))
        val resolver = LibreTranslateLanguageResolver(listOf(english, french))

        resolver.resolve(TranslationLanguageTag.require("en")) shouldBe english
        resolver.resolve(TranslationLanguageTag.require("en-US")) shouldBe english
        resolver.supportsTarget(english, french) shouldBe true
    }

    @Test
    fun `ambiguous base language fallback is rejected`() {
        val resolver = LibreTranslateLanguageResolver(
            listOf(
                language("pt-BR"),
                language("pt-PT"),
            ),
        )

        resolver.resolve(TranslationLanguageTag.require("pt")) shouldBe null
        resolver.resolve(TranslationLanguageTag.require("pt-BR"))?.code shouldBe "pt-BR"
    }

    @Test
    fun `target support comes from the source catalog`() {
        val english = language("en", targets = setOf("fr"))
        val polish = language("pl")

        LibreTranslateLanguageResolver(listOf(english, polish))
            .supportsTarget(english, polish) shouldBe false
    }

    private fun language(
        code: String,
        targets: Set<String> = emptySet(),
    ) = LibreTranslateLanguage(code, code, targets)
}
