package mihon.translation.runtime.preference

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.request.TranslationTargetLanguageSelection
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry

class ProfileTranslationPreferencesTest {

    @Test
    fun `profile owner declares only translation engine and target defaults`() {
        val registry = ProfilePreferenceOwnerRegistry()
        ProfilePreferenceOwnerInstaller(registry, ::InMemoryPreferenceStore).register(
            id = ProfilePreferenceOwnerId("translation"),
            factory = { ProfileTranslationPreferences(it, DEFAULT_ENGINE) },
        )

        registry.ownership().profileKeys shouldContainExactlyInAnyOrder setOf(
            "translation_engine",
            "translation_target_language",
        )
    }

    @Test
    fun `engine and target selections preserve provider-neutral identities`() {
        val preferences = ProfileTranslationPreferences(InMemoryPreferenceStore(), DEFAULT_ENGINE)
        val engine = TranslationEngineId("example.engine")
        val target = TranslationTargetLanguageSelection.Explicit(LanguageTag.require("pt-BR"))

        preferences.engine.set(engine)
        preferences.targetLanguage.set(target)

        preferences.engine.get() shouldBe engine
        preferences.targetLanguage.get() shouldBe target
    }

    private companion object {
        val DEFAULT_ENGINE = TranslationEngineId("android-system")
    }
}
