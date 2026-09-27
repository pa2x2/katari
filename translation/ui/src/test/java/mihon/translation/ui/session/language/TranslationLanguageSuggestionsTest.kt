package mihon.translation.ui.session.language

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.language.TranslationLanguagePair
import mihon.translation.api.language.TranslationLanguageSupport
import mihon.translation.api.preparation.TranslationPreparation
import mihon.translation.api.preparation.TranslationTargetChoiceReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import org.junit.jupiter.api.Test

class TranslationLanguageSuggestionsTest {
    @Test
    fun `text already in the target language is offered other supported targets, most recent first`() {
        val support = TranslationLanguageSupport.ExactPairs(
            setOf(ENGLISH to SPANISH, ENGLISH to JAPANESE, ENGLISH to GERMAN, ENGLISH to FRENCH).toPairs(),
        )

        suggestedLanguages(
            preparation = TranslationPreparation.TargetLanguageRequired(
                sourceLanguage = ENGLISH,
                reason = TranslationTargetChoiceReason.SourceEqualsTarget,
            ),
            recentLanguages = listOf(ENGLISH, SPANISH, KOREAN, JAPANESE, GERMAN, FRENCH),
            defaultTarget = ENGLISH,
            support = support,
        ) shouldBe listOf(SPANISH, JAPANESE, GERMAN)
    }

    @Test
    fun `an unsupported pair is offered the default and recent targets reachable from its source`() {
        val support = TranslationLanguageSupport.ExactPairs(
            setOf(FRENCH to ENGLISH, FRENCH to SPANISH, ENGLISH to KOREAN).toPairs(),
        )

        suggestedLanguages(
            preparation = TranslationPreparation.Unavailable(
                TranslationUnavailableReason.UnsupportedLanguagePair(source = FRENCH, target = KOREAN),
            ),
            recentLanguages = listOf(KOREAN, SPANISH),
            defaultTarget = ENGLISH,
            support = support,
        ) shouldBe listOf(ENGLISH, SPANISH)
    }

    @Test
    fun `undetermined text is offered its suggested languages before recent ones, once support is known`() {
        val preparation = TranslationPreparation.SourceUndetermined(suggestedLanguages = listOf(FRENCH, KOREAN))
        val support = TranslationLanguageSupport.ByRole(
            sourceLanguages = setOf(FRENCH, SPANISH, GERMAN),
            targetLanguages = setOf(ENGLISH),
        )

        suggestedLanguages(preparation, listOf(SPANISH, FRENCH), defaultTarget = ENGLISH, support = null) shouldBe
            emptyList()
        suggestedLanguages(preparation, listOf(SPANISH, FRENCH), defaultTarget = ENGLISH, support = support) shouldBe
            listOf(FRENCH, SPANISH)
    }

    private fun Set<Pair<LanguageTag, LanguageTag>>.toPairs() =
        mapTo(mutableSetOf()) { (source, target) -> TranslationLanguagePair(source, target) }

    private companion object {
        val ENGLISH = LanguageTag.require("en")
        val SPANISH = LanguageTag.require("es")
        val JAPANESE = LanguageTag.require("ja")
        val GERMAN = LanguageTag.require("de")
        val FRENCH = LanguageTag.require("fr")
        val KOREAN = LanguageTag.require("ko")
    }
}
