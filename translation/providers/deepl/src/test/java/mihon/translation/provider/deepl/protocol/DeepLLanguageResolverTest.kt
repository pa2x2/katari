package mihon.translation.provider.deepl.protocol

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test

class DeepLLanguageResolverTest {

    @Test
    fun `a regional language is translated into the closest variant the server lists`() {
        // As DeepL lists them: sources without regions, targets mostly with.
        val resolver = DeepLLanguageResolver(
            DeepLLanguages(
                sources = listOf("EN", "JA", "PT", "ZH").map { DeepLLanguage(it, it) },
                targets = listOf("EN-GB", "EN-US", "ES", "ES-419", "JA", "PT-BR", "PT-PT", "ZH", "ZH-HANS", "ZH-HANT")
                    .map { DeepLLanguage(it, it) },
            ),
        )

        // Traditional Chinese is not answered in simplified characters, nor Mexican Spanish in Spain's.
        resolver.target(tag("zh-TW")) shouldBe "ZH-HANT"
        resolver.target(tag("zh-Hant-HK")) shouldBe "ZH-HANT"
        resolver.target(tag("zh-CN")) shouldBe "ZH"
        resolver.target(tag("es-MX")) shouldBe "ES-419"
        resolver.target(tag("es-AD")) shouldBe "ES"
        // A server is never sent a code it does not list, such as a bare EN here.
        resolver.target(tag("en-AU")) shouldBe "EN-GB"
        resolver.target(tag("en")) shouldBe "EN-GB"
        resolver.target(tag("ko")) shouldBe null
        resolver.source(tag("en-US")) shouldBe "EN"
    }

    private fun tag(value: String) = LanguageTag.require(value)
}
