package mihon.tts.runtime.preference

import io.kotest.matchers.shouldBe
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.runtime.selection.ProfileTtsEngineResolver
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class ProfileTtsPreferencesTest {

    @Test
    fun `initial available engine becomes a persistent explicit profile choice`() {
        val initialEngine = TtsEngineId("initial-engine")
        val preferences = ProfileTtsPreferences(InMemoryPreferenceStore(), initialEngine)

        preferences.engine.isSet() shouldBe true
        ProfileTtsEngineResolver(preferences).resolve() shouldBe initialEngine
    }

    @Test
    fun `missing initial engine does not manufacture a selectable preference`() {
        val preferences = ProfileTtsPreferences(InMemoryPreferenceStore(), initialEngine = null)

        preferences.engine.isSet() shouldBe false
        ProfileTtsEngineResolver(preferences).resolve() shouldBe null
    }
}
