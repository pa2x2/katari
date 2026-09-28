package mihon.text.recognition.ui.languages

import io.kotest.matchers.shouldBe
import mihon.language.api.tag.LanguageTag
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.configuration.TextRecognitionPipelineOrigin
import mihon.text.recognition.api.configuration.TextRecognitionPipelineResolution
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import mihon.text.recognition.api.provider.TextRecognitionProviderId
import org.junit.jupiter.api.Test

class TextRecognitionLanguageOverviewTest {

    @Test
    fun `a regional override lists its language only as a choice, runnable or not`() {
        val chosen = TextRecognitionPipelineSelection.Preset(OTHER.id)
        val missing = TextRecognitionPipelineSelection.Preset(TextRecognitionPresetId("removed.engine"))
        val configuration = TextRecognitionConfiguration(
            provider = null,
            overrides = mapOf(LanguageTag.require("fr-CA") to chosen, JAPANESE to missing),
        )
        val resolutions = mapOf(
            LanguageTag.require("fr-CA") to TextRecognitionPipelineResolution.Resolved(
                OTHER.pipeline,
                TextRecognitionPipelineOrigin.Override,
                OTHER,
            ),
            JAPANESE to TextRecognitionPipelineResolution.OverrideUnavailable(missing),
            GERMAN to TextRecognitionPipelineResolution.Resolved(LATIN.pipeline, ENGINE, LATIN),
        )

        val overview = textRecognitionLanguageOverview(
            languages = listOf(FRENCH, JAPANESE, GERMAN),
            configuration = configuration,
            resolve = resolutions::getValue,
        )

        overview.choices shouldBe listOf(
            TextRecognitionLanguageChoice(LanguageTag.require("fr-CA"), chosen, OTHER.pipeline),
            TextRecognitionLanguageChoice(JAPANESE, missing, null),
        )
        overview.engineGroups shouldBe listOf(TextRecognitionLanguageGroup(LATIN, listOf(GERMAN)))
    }

    private companion object {
        val ENGINE = TextRecognitionPipelineOrigin.Engine
        val FRENCH = LanguageTag.require("fr")
        val GERMAN = LanguageTag.require("de")
        val JAPANESE = LanguageTag.require("ja")
        val LATIN = preset("onnx.latin", "onnx", listOf(FRENCH, GERMAN))
        val OTHER = preset("mlkit.comics", "mlkit", listOf(FRENCH))

        fun preset(id: String, provider: String, languages: List<LanguageTag>) = TextRecognitionPreset(
            id = TextRecognitionPresetId(id),
            provider = TextRecognitionProviderId(provider),
            displayName = id,
            description = id,
            languages = languages.toSet(),
            pipeline = TextRecognitionPipeline(
                TextRecognitionComponentId("$provider.detector"),
                TextRecognitionComponentId("$id.recognizer"),
            ),
        )
    }
}
