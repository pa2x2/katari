package mihon.translation.ui.picker

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import mihon.translation.api.TranslationLanguagePair
import mihon.translation.api.TranslationLanguageSupport
import mihon.translation.api.TranslationLanguageTag
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

    @Test
    fun `exact pairs expose every source and constrain targets by the staged source`() {
        val support = TranslationLanguageSupport.ExactPairs(
            setOf(
                TranslationLanguagePair(ENGLISH, POLISH),
                TranslationLanguagePair(GERMAN, POLISH),
                TranslationLanguagePair(ENGLISH, FRENCH),
                TranslationLanguagePair(SPANISH, FRENCH),
            ),
        )

        support.selectableLanguages(TranslationLanguageRole.Source, POLISH)
            .shouldContainExactlyInAnyOrder(ENGLISH, GERMAN, SPANISH)
        support.selectableLanguages(TranslationLanguageRole.Target, ENGLISH)
            .shouldContainExactlyInAnyOrder(POLISH, FRENCH)
        support.selectableLanguages(TranslationLanguageRole.Target, SPANISH)
            .shouldContainExactlyInAnyOrder(FRENCH)
        support.selectableLanguages(TranslationLanguageRole.Target, ITALIAN)
            .shouldContainExactlyInAnyOrder(POLISH, FRENCH)
        support.supportsPair(ENGLISH, POLISH) shouldBe true
        support.supportsPair(GERMAN, FRENCH) shouldBe false
    }

    @Test
    fun `role support filters independently while broad support uses the global catalog`() {
        val byRole = TranslationLanguageSupport.ByRole(
            sourceLanguages = setOf(ENGLISH, GERMAN),
            targetLanguages = setOf(POLISH),
        )

        byRole.selectableLanguages(TranslationLanguageRole.Source, POLISH)
            .shouldContainExactlyInAnyOrder(ENGLISH, GERMAN)
        byRole.selectableLanguages(TranslationLanguageRole.Target, ENGLISH)
            .shouldContainExactlyInAnyOrder(POLISH)
        byRole.supportsPair(GERMAN, POLISH) shouldBe true

        translationLanguageOptions(
            support = TranslationLanguageSupport.AnyLanguage,
            role = TranslationLanguageRole.Target,
            counterpart = ENGLISH,
            availableLocales = arrayOf(Locale.ENGLISH, Locale.FRENCH),
            displayLocale = Locale.ENGLISH,
        ).map { it.tag }.shouldContainExactlyInAnyOrder(ENGLISH, FRENCH)
    }

    private companion object {
        val ENGLISH = TranslationLanguageTag.require("en")
        val FRENCH = TranslationLanguageTag.require("fr")
        val GERMAN = TranslationLanguageTag.require("de")
        val ITALIAN = TranslationLanguageTag.require("it")
        val POLISH = TranslationLanguageTag.require("pl")
        val SPANISH = TranslationLanguageTag.require("es")
    }
}
