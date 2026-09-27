package mihon.text.recognition.runtime.selection

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import mihon.text.recognition.api.provider.TextRecognitionProviderId
import org.junit.jupiter.api.Test
import tachiyomi.core.common.preference.InMemoryPreferenceStore

class ProfileTextRecognitionPreferencesTest {

    @Test
    fun `saving a configuration replaces the previous engine and overrides`() {
        val preferences = ProfileTextRecognitionPreferences(InMemoryPreferenceStore())
        val japanese = TextRecognitionPipelineSelection.Preset(TextRecognitionPresetId("onnx.japanese-manga"))
        val english = TextRecognitionPipelineSelection.Preset(TextRecognitionPresetId("onnx.comics-english"))
        preferences.save(
            TextRecognitionConfiguration(
                provider = TextRecognitionProviderId("onnx"),
                overrides = mapOf(LanguageTag.require("ja-JP") to japanese, LanguageTag.require("en") to english),
            ),
        )

        preferences.save(
            TextRecognitionConfiguration(provider = null, overrides = mapOf(LanguageTag.require("en") to english)),
        )

        preferences.configuration() shouldBe TextRecognitionConfiguration(
            provider = null,
            overrides = mapOf(LanguageTag.require("en") to english),
        )
    }

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
