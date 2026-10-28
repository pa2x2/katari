package mihon.desktop.textrecognition

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.configuration.TextRecognitionPipelineResolution
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.api.host.TextRecognitionPlatformModelsResult
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider

/**
 * Recognition settings with nothing to configure: [providers] is empty, which is how a platform reports that it doesn't
 * recognize text, and no language is read. Stands in until desktop has a recognition engine.
 */
@Inject
class NoTextRecognitionHostActions : TextRecognitionHostActions {
    override val providers: List<KnownTextRecognitionProvider> = emptyList()
    override val knownComponents: List<KnownTextRecognitionComponent> = emptyList()
    override val presets: List<TextRecognitionPreset> = emptyList()
    override val supportedLanguages: List<LanguageTag> = emptyList()

    override fun observeConfiguration(): Flow<TextRecognitionConfiguration> =
        flowOf(TextRecognitionConfiguration(provider = null))

    override fun saveConfiguration(configuration: TextRecognitionConfiguration) = Unit

    override fun resolve(
        configuration: TextRecognitionConfiguration,
        language: LanguageTag,
    ): TextRecognitionPipelineResolution = TextRecognitionPipelineResolution.UnsupportedLanguage

    override fun presets(language: LanguageTag): List<TextRecognitionPreset> = emptyList()

    override fun pipelines(language: LanguageTag): List<TextRecognitionPipeline> = emptyList()

    override fun pipeline(selection: TextRecognitionPipelineSelection): TextRecognitionPipeline? = null

    override fun models(pipeline: TextRecognitionPipeline, language: LanguageTag): List<ModelArtifactDescriptor> =
        emptyList()

    override suspend fun installPlatformModels(
        component: TextRecognitionComponentId,
        language: LanguageTag,
    ): TextRecognitionPlatformModelsResult = TextRecognitionPlatformModelsResult.Failed(
        "Desktop has no recognition engine whose models the platform installs",
    )
}
