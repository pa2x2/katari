package mihon.text.recognition.runtime.host

import kotlinx.coroutines.flow.Flow
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
import mihon.text.recognition.runtime.registry.TextRecognitionComponentRegistry
import mihon.text.recognition.runtime.selection.ProfileTextRecognitionPreferences
import mihon.text.recognition.runtime.selection.TextRecognitionPipelineResolver
import mihon.text.recognition.spi.component.TextRecognitionPlatformModelsInstallation

internal class DefaultTextRecognitionHostActions(
    private val registry: TextRecognitionComponentRegistry,
    private val preferences: ProfileTextRecognitionPreferences,
    private val resolver: TextRecognitionPipelineResolver,
) : TextRecognitionHostActions {
    override val providers: List<KnownTextRecognitionProvider>
        get() = registry.providers

    override val knownComponents: List<KnownTextRecognitionComponent>
        get() = registry.knownComponents

    override val presets: List<TextRecognitionPreset>
        get() = registry.presets

    override val supportedLanguages: List<LanguageTag>
        get() = registry.supportedLanguages

    override fun observeConfiguration(): Flow<TextRecognitionConfiguration> = preferences.observeConfiguration()

    override fun saveConfiguration(configuration: TextRecognitionConfiguration) = preferences.save(configuration)

    override fun resolve(
        configuration: TextRecognitionConfiguration,
        language: LanguageTag,
    ): TextRecognitionPipelineResolution = resolver.resolve(configuration, language)

    override fun presets(language: LanguageTag): List<TextRecognitionPreset> = registry.presets(language)

    override fun pipelines(language: LanguageTag): List<TextRecognitionPipeline> = registry.pipelines(language)

    override fun pipeline(selection: TextRecognitionPipelineSelection): TextRecognitionPipeline? =
        resolver.pipeline(selection)

    override fun models(pipeline: TextRecognitionPipeline, language: LanguageTag): List<ModelArtifactDescriptor> =
        pipeline.components.mapNotNull(registry::component).flatMap { it.models(language) }.distinct()

    override suspend fun installPlatformModels(
        component: TextRecognitionComponentId,
        language: LanguageTag,
    ): TextRecognitionPlatformModelsResult {
        val installed = registry.component(component)
            ?: return TextRecognitionPlatformModelsResult.Failed("${component.value} is not part of this build")
        return when (val installation = installed.installPlatformModels(language)) {
            TextRecognitionPlatformModelsInstallation.Installed -> TextRecognitionPlatformModelsResult.Installed
            is TextRecognitionPlatformModelsInstallation.Failed ->
                TextRecognitionPlatformModelsResult.Failed(installation.reason)
        }
    }
}
