package mihon.translation.runtime.language

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.request.TranslationTargetLanguageSelection
import mihon.translation.runtime.preference.ProfileTranslationPreferences
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import java.util.Locale

class ProfileTranslationDefaultTargetLanguageResolverTest {

    @Test
    fun `unset profile target follows the effective UI locale dynamically`() {
        val preferences = ProfileTranslationPreferences(InMemoryPreferenceStore(), DEFAULT_ENGINE)
        var locale = Locale.forLanguageTag("pl-PL")
        val resolver = ProfileTranslationDefaultTargetLanguageResolver(preferences) { locale }

        resolver.resolve() shouldBe LanguageTag.require("pl-PL")
        locale = Locale.forLanguageTag("de-DE")
        resolver.resolve() shouldBe LanguageTag.require("de-DE")
    }

    @Test
    fun `explicit profile target wins over the effective UI locale`() {
        val preferences = ProfileTranslationPreferences(InMemoryPreferenceStore(), DEFAULT_ENGINE)
        preferences.targetLanguage.set(
            TranslationTargetLanguageSelection.Explicit(LanguageTag.require("es")),
        )
        val resolver = ProfileTranslationDefaultTargetLanguageResolver(preferences) {
            Locale.forLanguageTag("pl-PL")
        }

        resolver.resolve() shouldBe LanguageTag.require("es")
    }

    private companion object {
        val DEFAULT_ENGINE = TranslationEngineId("android-system")
    }
}
