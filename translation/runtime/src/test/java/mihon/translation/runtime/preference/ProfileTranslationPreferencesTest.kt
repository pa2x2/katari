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
            "translation_recent_languages",
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

    @Test
    fun `recent languages round-trip as an ordered distinct list`() {
        val preferences = ProfileTranslationPreferences(InMemoryPreferenceStore(), DEFAULT_ENGINE)

        preferences.recentLanguages.set(listOf(SPANISH, SIMPLIFIED_CHINESE, PORTUGUESE_BRAZIL))

        preferences.recentLanguages.get() shouldBe listOf(SPANISH, SIMPLIFIED_CHINESE, PORTUGUESE_BRAZIL)
    }

    @Test
    fun `recent language use moves the language to the front and caps the list`() {
        val recents = listOf(SPANISH, SIMPLIFIED_CHINESE, PORTUGUESE_BRAZIL)

        recents.withRecentUse(SIMPLIFIED_CHINESE, limit = 3) shouldBe
            listOf(SIMPLIFIED_CHINESE, SPANISH, PORTUGUESE_BRAZIL)
        recents.withRecentUse(PORTUGUESE_BRAZIL, limit = 2) shouldBe listOf(PORTUGUESE_BRAZIL, SPANISH)
    }

    @Test
    fun `recent language use appends unseen languages`() {
        emptyList<LanguageTag>().withRecentUse(SPANISH) shouldBe listOf(SPANISH)
        listOf(SPANISH).withRecentUse(SIMPLIFIED_CHINESE) shouldBe listOf(SIMPLIFIED_CHINESE, SPANISH)
    }

    private companion object {
        val DEFAULT_ENGINE = TranslationEngineId("android-system")
        val SPANISH = LanguageTag.require("es")
        val SIMPLIFIED_CHINESE = LanguageTag.require("zh-Hans")
        val PORTUGUESE_BRAZIL = LanguageTag.require("pt-BR")
    }
}
