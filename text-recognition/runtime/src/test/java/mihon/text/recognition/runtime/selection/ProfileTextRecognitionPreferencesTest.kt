package mihon.text.recognition.runtime.selection

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class ProfileTextRecognitionPreferencesTest {

    @Test
    fun `regional variants of a language share one stored choice`() {
        val preferences = ProfileTextRecognitionPreferences(InMemoryPreferenceStore())

        preferences.selection(LanguageTag.require("ja-JP")).key() shouldBe
            preferences.selection(LanguageTag.require("ja")).key()
    }
}
