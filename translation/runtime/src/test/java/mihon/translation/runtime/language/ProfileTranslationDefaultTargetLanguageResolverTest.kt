package mihon.translation.runtime

import io.kotest.matchers.shouldBe
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationTargetLanguageSelection
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.util.Locale

class ProfileTranslationDefaultTargetLanguageResolverTest {

    @Test
    fun `unset profile target follows the effective UI locale dynamically`() {
        val preferences = ProfileTranslationPreferences(InMemoryPreferenceStore())
        var locale = Locale.forLanguageTag("pl-PL")
        val resolver = ProfileTranslationDefaultTargetLanguageResolver(preferences) { locale }

        resolver.resolve() shouldBe TranslationLanguageTag.require("pl-PL")
        locale = Locale.forLanguageTag("de-DE")
        resolver.resolve() shouldBe TranslationLanguageTag.require("de-DE")
    }

    @Test
    fun `explicit profile target wins over the effective UI locale`() {
        val preferences = ProfileTranslationPreferences(InMemoryPreferenceStore())
        preferences.targetLanguage.set(
            TranslationTargetLanguageSelection.Explicit(TranslationLanguageTag.require("es")),
        )
        val resolver = ProfileTranslationDefaultTargetLanguageResolver(preferences) {
            Locale.forLanguageTag("pl-PL")
        }

        resolver.resolve() shouldBe TranslationLanguageTag.require("es")
    }
}
