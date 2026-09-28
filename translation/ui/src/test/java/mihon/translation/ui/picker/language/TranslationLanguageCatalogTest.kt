package mihon.translation.ui.picker.language

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.language.TranslationLanguagePair
import mihon.translation.api.language.TranslationLanguageSupport
import org.junit.jupiter.api.Test

class TranslationLanguageCatalogTest {
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
    fun `exact pairs report targets reachable only from other sources as unpairable`() {
        val support = TranslationLanguageSupport.ExactPairs(
            setOf(
                TranslationLanguagePair(ENGLISH, POLISH),
                TranslationLanguagePair(ENGLISH, FRENCH),
                TranslationLanguagePair(SPANISH, FRENCH),
                TranslationLanguagePair(FRENCH, ENGLISH),
            ),
        )

        support.unpairableLanguages(TranslationLanguageRole.Target, SPANISH)
            .shouldContainExactlyInAnyOrder(POLISH, ENGLISH)
        support.unpairableLanguages(TranslationLanguageRole.Target, FRENCH)
            .shouldContainExactlyInAnyOrder(POLISH)
        support.unpairableLanguages(TranslationLanguageRole.Target, ITALIAN) shouldBe emptySet()
        support.unpairableLanguages(TranslationLanguageRole.Target, null) shouldBe emptySet()
        support.unpairableLanguages(TranslationLanguageRole.Source, POLISH) shouldBe emptySet()
    }

    private companion object {
        val ENGLISH = LanguageTag.require("en")
        val FRENCH = LanguageTag.require("fr")
        val GERMAN = LanguageTag.require("de")
        val ITALIAN = LanguageTag.require("it")
        val POLISH = LanguageTag.require("pl")
        val SPANISH = LanguageTag.require("es")
    }
}
