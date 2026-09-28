package mihon.text.recognition.runtime.selection

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class ProfileTextRecognitionPreferencesTest {

    @Test
    fun `regional variants of a language share one override`() {
        val preferences = ProfileTextRecognitionPreferences(InMemoryPreferenceStore())
        val selection = TextRecognitionPipelineSelection.Preset(TextRecognitionPresetId("onnx.japanese-manga"))

        preferences.save(
            TextRecognitionConfiguration(provider = null, overrides = mapOf(LanguageTag.require("ja-JP") to selection)),
        )

        preferences.configuration().overrides shouldBe mapOf(LanguageTag.require("ja") to selection)
    }
}
