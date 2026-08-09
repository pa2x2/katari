package mihon.translation.provider.libretranslate.protocol

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.language.TranslationLanguagePair
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.language.TranslationLanguageSupportInspection
import org.junit.jupiter.api.Test

class LibreTranslateLanguageResolverTest {

    @Test
    fun `exact tags win and unique base language fallback is allowed`() {
        val english = language("en", targets = setOf("fr"))
        val french = language("fr", targets = setOf("en"))
        val resolver = LibreTranslateLanguageResolver(listOf(english, french))

        resolver.resolve(LanguageTag.require("en")) shouldBe english
        resolver.resolve(LanguageTag.require("en-US")) shouldBe english
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

        resolver.resolve(LanguageTag.require("pt")) shouldBe null
        resolver.resolve(LanguageTag.require("pt-BR"))?.code shouldBe "pt-BR"
    }

    @Test
    fun `target support comes from the source catalog`() {
        val english = language("en", targets = setOf("fr"))
        val polish = language("pl")

        LibreTranslateLanguageResolver(listOf(english, polish))
            .supportsTarget(english, polish) shouldBe false
    }

    @Test
    fun `language support preserves asymmetric provider pairs and omits invalid tags`() {
        val english = language("en", targets = setOf("fr", "pt-BR"))
        val french = language("fr", targets = setOf("en"))
        val brazilianPortuguese = language("pt-BR", targets = setOf("en"))
        val invalid = language("und", targets = setOf("en"))

        val inspection = LibreTranslateLanguageResolver(
            listOf(english, french, brazilianPortuguese, invalid),
        ).languageSupport() as TranslationLanguageSupportInspection.Available
        val pairs = (inspection.support as TranslationLanguageSupport.ExactPairs).pairs

        pairs shouldBe setOf(
            TranslationLanguagePair(ENGLISH, FRENCH),
            TranslationLanguagePair(ENGLISH, BRAZILIAN_PORTUGUESE),
            TranslationLanguagePair(FRENCH, ENGLISH),
            TranslationLanguagePair(BRAZILIAN_PORTUGUESE, ENGLISH),
        )
    }

    private fun language(
        code: String,
        targets: Set<String> = emptySet(),
    ) = LibreTranslateLanguage(code, code, targets)

    private companion object {
        val ENGLISH = LanguageTag.require("en")
        val FRENCH = LanguageTag.require("fr")
        val BRAZILIAN_PORTUGUESE = LanguageTag.require("pt-BR")
    }
}
