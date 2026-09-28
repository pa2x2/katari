package mihon.text.recognition.ui.fixture

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.descriptor.ModelArtifactFile
import mihon.model.artifacts.api.descriptor.ModelArtifactHosting
import mihon.model.artifacts.api.descriptor.ModelArtifactId
import mihon.model.artifacts.api.descriptor.ModelArtifactLicense
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

/**
 * One engine reading Japanese with a manga recognizer and English with a general one, both after a shared bubble
 * detector. Each component needs one model. Choices resolve like the runtime: by primary language, and unavailable
 * when they name a preset the catalog lacks.
 */
internal class MangaCatalogHostActions : TextRecognitionHostActions {
    override val providers = listOf(ENGINE)
    override val knownComponents = listOf(DETECTOR, MANGA_OCR, GENERAL_OCR)
    override val presets = listOf(JAPANESE_MANGA, ENGLISH_COMICS)
    override val supportedLanguages = listOf(JAPANESE, ENGLISH)

    override fun observeConfiguration(): Flow<TextRecognitionConfiguration> = emptyFlow()

    override fun saveConfiguration(configuration: TextRecognitionConfiguration) = Unit

    override fun resolve(
        configuration: TextRecognitionConfiguration,
        language: LanguageTag,
    ): TextRecognitionPipelineResolution {
        val choice = configuration.overrides.entries
            .firstOrNull { it.key.value.substringBefore('-') == language.value }
            ?.value
            ?: return presets(language).first().let {
                TextRecognitionPipelineResolution.Resolved(it.pipeline, TextRecognitionPipelineOrigin.Engine, it)
            }
        return pipeline(choice)
            ?.let { TextRecognitionPipelineResolution.Resolved(it, TextRecognitionPipelineOrigin.Override) }
            ?: TextRecognitionPipelineResolution.OverrideUnavailable(choice)
    }

    override fun presets(language: LanguageTag) = presets.filter { language in it.languages }

    override fun pipelines(language: LanguageTag) = knownComponents
        .filter { it.role == TextRecognitionComponentRole.Recognizer && language in it.languages }
        .map { TextRecognitionPipeline(DETECTOR.id, it.id) }

    override fun pipeline(selection: TextRecognitionPipelineSelection): TextRecognitionPipeline? =
        when (selection) {
            is TextRecognitionPipelineSelection.Preset -> presets.firstOrNull { it.id == selection.preset }?.pipeline
            is TextRecognitionPipelineSelection.Custom -> selection.pipeline
        }

    override fun models(pipeline: TextRecognitionPipeline, language: LanguageTag) =
        pipeline.components.map(MODELS::getValue)

    override suspend fun installPlatformModels(
        component: TextRecognitionComponentId,
        language: LanguageTag,
    ) = TextRecognitionPlatformModelsResult.Installed

    companion object {
        private val SHA256 = "0".repeat(64)
        val JAPANESE = LanguageTag.require("ja")
        val ENGLISH = LanguageTag.require("en")
        val ENGINE = KnownTextRecognitionProvider(
            id = TextRecognitionProviderId("onnx"),
            name = "On-device models",
            description = "Open models",
            processingLocation = "On this device",
            buildAvailability = TextRecognitionBuildAvailability.Included,
        )
        val DETECTOR = component("onnx.detector", TextRecognitionComponentRole.Detector, emptySet())
        val MANGA_OCR = component("onnx.manga-ocr", TextRecognitionComponentRole.Recognizer, setOf(JAPANESE))
        val GENERAL_OCR = component("onnx.general", TextRecognitionComponentRole.Recognizer, setOf(JAPANESE, ENGLISH))
        val JAPANESE_MANGA = preset("onnx.japanese-manga", JAPANESE, MANGA_OCR)
        val ENGLISH_COMICS = preset("onnx.english-comics", ENGLISH, GENERAL_OCR)
        val DETECTOR_MODEL = model("onnx.detector")
        val MANGA_OCR_MODEL = model("onnx.manga-ocr")
        val GENERAL_OCR_MODEL = model("onnx.general")
        private val MODELS = mapOf(
            DETECTOR.id to DETECTOR_MODEL,
            MANGA_OCR.id to MANGA_OCR_MODEL,
            GENERAL_OCR.id to GENERAL_OCR_MODEL,
        )

        private fun component(id: String, role: TextRecognitionComponentRole, languages: Set<LanguageTag>) =
            KnownTextRecognitionComponent(
                id = TextRecognitionComponentId(id),
                provider = ENGINE.id,
                role = role,
                displayName = id,
                description = id,
                languages = languages,
            )

        private fun preset(id: String, language: LanguageTag, recognizer: KnownTextRecognitionComponent) =
            TextRecognitionPreset(
                id = TextRecognitionPresetId(id),
                provider = ENGINE.id,
                displayName = id,
                description = id,
                languages = setOf(language),
                pipeline = TextRecognitionPipeline(DETECTOR.id, recognizer.id),
            )

        private fun model(id: String) = ModelArtifactDescriptor(
            id = ModelArtifactId(id),
            revision = "1",
            displayName = id,
            files = listOf(
                ModelArtifactFile(name = "model.onnx", url = "https://example.org/$id", sizeBytes = 1, sha256 = SHA256),
            ),
            license = ModelArtifactLicense(name = "MIT", url = "https://example.org/license"),
            hosting = ModelArtifactHosting.Upstream("https://example.org/$id"),
        )
    }
}
