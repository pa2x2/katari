package mihon.tts.runtime.host

import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.engine.TtsProviderId
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.runtime.preference.ProfileTtsPreferences
import mihon.tts.runtime.registry.DefaultTtsEngineRegistry
import mihon.tts.runtime.selection.ProfileTtsEngineResolver
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class DefaultTtsHostActionsTest {

    @Test
    fun `voice overrides remain enumerable and reset removes their profile key`() {
        val fixture = fixture()
        val language = LanguageTag.require("pt-BR")
        val voice = TtsVoiceId(PROVIDER_ID, DEFAULT_ENGINE, "pt-br-local")

        fixture.hostActions.setSelectedVoice(language, voice)

        fixture.hostActions.selectedVoiceOverrides() shouldContainExactly mapOf(language to voice)

        fixture.hostActions.setSelectedVoice(language, null)

        fixture.hostActions.selectedVoiceOverrides() shouldBe emptyMap()
        fixture.preferences.voice(language).isSet() shouldBe false
    }

    private fun fixture(): Fixture {
        val store = InMemoryPreferenceStore()
        val preferences = ProfileTtsPreferences(store, DEFAULT_ENGINE)
        val registry = DefaultTtsEngineRegistry(emptyList())
        return Fixture(
            preferences = preferences,
            hostActions = DefaultTtsHostActions(
                preferences = preferences,
                engineRegistry = registry,
                catalog = registry,
                setupRegistry = registry,
                engineResolver = ProfileTtsEngineResolver(preferences),
            ),
        )
    }

    private data class Fixture(
        val preferences: ProfileTtsPreferences,
        val hostActions: DefaultTtsHostActions,
    )

    private companion object {
        val DEFAULT_ENGINE = TtsEngineId("android-default")
        val PROVIDER_ID = TtsProviderId("android-tts")
    }
}
