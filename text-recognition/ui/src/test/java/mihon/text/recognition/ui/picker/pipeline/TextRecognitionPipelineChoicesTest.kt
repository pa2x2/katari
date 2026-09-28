package mihon.text.recognition.ui.picker.pipeline

import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.component.TextRecognitionComponentRole
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.configuration.TextRecognitionPipelineOrigin
import mihon.text.recognition.api.configuration.TextRecognitionPipelineResolution
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.api.host.TextRecognitionPlatformModelsResult
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.pipeline.TextRecognitionPresetId
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.api.provider.TextRecognitionProviderId
import org.junit.jupiter.api.Test

class TextRecognitionPipelineChoicesTest {

    @Test
    fun `combinations a preset already offers are listed only as that preset`() {
        val choices = HOST.pipelineChoices(TextRecognitionConfiguration(ENGINE.id), JAPANESE, emptyMap())

        choices.presets.map { it.selection } shouldBe listOf(TextRecognitionPipelineSelection.Preset(MANGA.id))
        choices.custom.map { it.pipeline } shouldBe listOf(OTHER_PIPELINE)
    }

    @Test
    fun `automatic names what the engine picks even while the language has a choice of its own`() {
        val chosen = TextRecognitionPipelineSelection.Custom(OTHER_PIPELINE)
        val configuration = TextRecognitionConfiguration(ENGINE.id, mapOf(LanguageTag.require("ja-JP") to chosen))

        val choices = HOST.pipelineChoices(configuration, JAPANESE, emptyMap())

        choices.automatic?.option?.selection shouldBe TextRecognitionPipelineSelection.Preset(MANGA.id)
        choices.automatic?.recommendedBy shouldBe ENGINE
        choices.current shouldBe chosen
        choices.currentUnavailable shouldBe false
    }

    @Test
    fun `a choice naming a removed preset is reported so the picker can ask for a new one`() {
        val removed = TextRecognitionPipelineSelection.Preset(TextRecognitionPresetId("removed.engine"))
        val configuration = TextRecognitionConfiguration(ENGINE.id, mapOf(JAPANESE to removed))

        val choices = HOST.pipelineChoices(configuration, JAPANESE, emptyMap())

        choices.current shouldBe removed
        choices.currentUnavailable shouldBe true
    }

    private companion object {
        val JAPANESE = LanguageTag.require("ja")
        val ENGINE = KnownTextRecognitionProvider(
            id = TextRecognitionProviderId("onnx"),
            name = "On-device models",
            description = "Open models",
            processingLocation = "On this device",
            buildAvailability = TextRecognitionBuildAvailability.Included,
        )
        val DETECTOR = component("onnx.detector", TextRecognitionComponentRole.Detector, emptySet())
        val MANGA_OCR = component("onnx.manga-ocr", TextRecognitionComponentRole.Recognizer, setOf(JAPANESE))
        val OTHER_OCR = component("onnx.other", TextRecognitionComponentRole.Recognizer, setOf(JAPANESE))
        val MANGA = TextRecognitionPreset(
            id = TextRecognitionPresetId("onnx.japanese-manga"),
            provider = ENGINE.id,
            displayName = "Japanese manga",
            description = "Reads manga",
            languages = setOf(JAPANESE),
            pipeline = TextRecognitionPipeline(DETECTOR.id, MANGA_OCR.id),
        )
        val OTHER_PIPELINE = TextRecognitionPipeline(DETECTOR.id, OTHER_OCR.id)
        val HOST = PipelineChoicesHost()

        fun component(id: String, role: TextRecognitionComponentRole, languages: Set<LanguageTag>) =
            KnownTextRecognitionComponent(
                id = TextRecognitionComponentId(id),
                provider = ENGINE.id,
                role = role,
                displayName = id,
                description = id,
                languages = languages,
            )
    }

    /** One engine with one Japanese preset; an override resolves when it names a pipeline the catalog has. */
    private class PipelineChoicesHost : TextRecognitionHostActions {
        override val providers = listOf(ENGINE)
        override val knownComponents = listOf(DETECTOR, MANGA_OCR, OTHER_OCR)
        override val presets = listOf(MANGA)
        override val supportedLanguages = listOf(JAPANESE)

        override fun observeConfiguration(): Flow<TextRecognitionConfiguration> = emptyFlow()

        override fun saveConfiguration(configuration: TextRecognitionConfiguration) = Unit

        override fun resolve(
            configuration: TextRecognitionConfiguration,
            language: LanguageTag,
        ): TextRecognitionPipelineResolution {
            val choice = configuration.overrides.entries
                .firstOrNull { it.key.value.substringBefore('-') == language.value }
                ?.value
            if (choice == null) {
                return TextRecognitionPipelineResolution.Resolved(
                    MANGA.pipeline,
                    TextRecognitionPipelineOrigin.Engine,
                    MANGA,
                )
            }
            return pipeline(choice)
                ?.let { TextRecognitionPipelineResolution.Resolved(it, TextRecognitionPipelineOrigin.Override) }
                ?: TextRecognitionPipelineResolution.OverrideUnavailable(choice)
        }

        override fun presets(language: LanguageTag) = presets

        override fun pipelines(language: LanguageTag) = listOf(MANGA.pipeline, OTHER_PIPELINE)

        override fun pipeline(selection: TextRecognitionPipelineSelection): TextRecognitionPipeline? =
            when (selection) {
                is TextRecognitionPipelineSelection.Preset -> presets.firstOrNull {
                    it.id == selection.preset
                }?.pipeline
                is TextRecognitionPipelineSelection.Custom -> selection.pipeline
            }

        override fun models(pipeline: TextRecognitionPipeline, language: LanguageTag) =
            emptyList<ModelArtifactDescriptor>()

        override suspend fun installPlatformModels(
            component: TextRecognitionComponentId,
            language: LanguageTag,
        ) = TextRecognitionPlatformModelsResult.Installed
    }
}
