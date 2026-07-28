package mihon.translation.runtime

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore
import tachiyomi.core.common.preference.Preference

class DeviceTranslationPreferencesTest {

    @Test
    fun `model network policy and disclosure state are device defaults`() {
        val preferences = DeviceTranslationPreferences(InMemoryPreferenceStore())

        preferences.wifiOnlyModelDownloads.get() shouldBe true
        preferences.mlKitDisclosureAcknowledged.get() shouldBe false
        preferences.mlKitDisclosureAcknowledged.key() shouldBe
            Preference.appStateKey("translation_mlkit_disclosure_acknowledged")
    }
}
