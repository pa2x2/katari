package eu.kanade.presentation.more.settings.screen.translation

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import java.util.Locale

class TranslationLanguageCatalogTest {

    @Test
    fun `region variants collapse while script variants remain distinct`() {
        val options = translationLanguageOptions(
            availableLocales = arrayOf(
                Locale.ROOT,
                Locale.US,
                Locale.UK,
                Locale.forLanguageTag("zh-Hans-CN"),
                Locale.forLanguageTag("zh-Hant-TW"),
            ),
            displayLocale = Locale.ENGLISH,
        )

        options.map { it.tag.value }.shouldContainExactlyInAnyOrder(
            "en",
            "zh-Hans",
            "zh-Hant",
        )
        options.map { it.tag.value }.distinct().size shouldBe options.size
    }
}
