package mihon.translation.runtime.language

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.language.TranslationDefaultTarget
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

        resolver.resolve() shouldBe TranslationDefaultTarget(LanguageTag.require("pl-PL"), followsAppLanguage = true)
        locale = Locale.forLanguageTag("de-DE")
        resolver.resolve() shouldBe TranslationDefaultTarget(LanguageTag.require("de-DE"), followsAppLanguage = true)
    }

    private companion object {
        val DEFAULT_ENGINE = TranslationEngineId("android-system")
    }
}
