package mihon.translation.runtime

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import mihon.translation.api.TranslationEngineId
import mihon.translation.api.TranslationEngineSelection
import mihon.translation.api.TranslationLanguageTag
import mihon.translation.api.TranslationTargetLanguageSelection
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.ProfilePreferenceOwnerId
import tachiyomi.core.common.preference.ProfilePreferenceOwnerInstaller
import tachiyomi.core.common.preference.ProfilePreferenceOwnerRegistry

class ProfileTranslationPreferencesTest {

    @Test
    fun `profile owner declares only engine target and future reader opt-in`() {
        val registry = ProfilePreferenceOwnerRegistry()
        ProfilePreferenceOwnerInstaller(registry, ::InMemoryPreferenceStore).register(
            id = ProfilePreferenceOwnerId("translation"),
            factory = ::ProfileTranslationPreferences,
        )

        registry.ownership().profileKeys shouldContainExactlyInAnyOrder setOf(
            "translation_engine",
            "translation_target_language",
            "translation_automatic_selection_enabled",
        )
    }

    @Test
    fun `engine and target selections preserve provider-neutral identities`() {
        val preferences = ProfileTranslationPreferences(InMemoryPreferenceStore())
        val engine = TranslationEngineSelection.Explicit(TranslationEngineId("example.engine"))
        val target = TranslationTargetLanguageSelection.Explicit(TranslationLanguageTag.require("pt-BR"))

        preferences.engineSelection.set(engine)
        preferences.targetLanguage.set(target)

        preferences.engineSelection.get() shouldBe engine
        preferences.targetLanguage.get() shouldBe target
        preferences.automaticSelectionTranslationEnabled.get() shouldBe false
    }
}
