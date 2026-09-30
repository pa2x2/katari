package mihon.translation.provider.deepl.protocol

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

class DeepLLanguageResolverTest {

    @Test
    fun `a language is translated into even when the server lists only regional variants of it`() {
        // As DeepL lists them: sources without regions, targets mostly with.
        val resolver = DeepLLanguageResolver(
            DeepLLanguages(
                sources = listOf("EN", "JA", "PT", "ZH").map { DeepLLanguage(it, it) },
                targets = listOf("EN-GB", "EN-US", "JA", "PT-BR", "PT-PT", "ZH-HANS", "ZH-HANT", "ES", "ES-419")
                    .map { DeepLLanguage(it, it) },
            ),
        )

        resolver.target(tag("en")) shouldBe "EN"
        resolver.target(tag("en-GB")) shouldBe "EN-GB"
        resolver.target(tag("en-AU")) shouldBe "EN"
        resolver.target(tag("zh-Hant")) shouldBe "ZH-HANT"
        resolver.target(tag("es-MX")) shouldBe "ES"
        resolver.target(tag("ja-JP")) shouldBe "JA"
        resolver.target(tag("ko")) shouldBe null
        resolver.source(tag("en-US")) shouldBe "EN"
    }

    private fun tag(value: String) = LanguageTag.require(value)
}
