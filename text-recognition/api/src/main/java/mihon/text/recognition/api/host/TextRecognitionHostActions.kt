package mihon.text.recognition.api.host

import kotlinx.coroutines.flow.Flow
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.configuration.TextRecognitionPipelineResolution
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider

/** Provider-neutral boundary shared by settings and reader surfaces. */
interface TextRecognitionHostActions {
    val providers: List<KnownTextRecognitionProvider>
    val knownComponents: List<KnownTextRecognitionComponent>
    val presets: List<TextRecognitionPreset>

    /** Languages at least one component of this build reads, in catalog order. */
    val supportedLanguages: List<LanguageTag>

    fun observeConfiguration(): Flow<TextRecognitionConfiguration>

    fun saveConfiguration(configuration: TextRecognitionConfiguration)

    /** How [configuration] reads [language]. Works for unsaved configurations, for example a settings draft. */
    fun resolve(configuration: TextRecognitionConfiguration, language: LanguageTag): TextRecognitionPipelineResolution

    /** Presets that read [language], including presets of engines this build excludes. */
    fun presets(language: LanguageTag): List<TextRecognitionPreset>

    /**
     * Every pipeline the catalog can form for [language]: each detector with each recognizer that reads it, and each
     * engine that reads it. Pipelines with excluded components are included so hosts can explain them.
     */
    fun pipelines(language: LanguageTag): List<TextRecognitionPipeline>

    /** Pipeline a selection names, or `null` when it names an unknown preset. */
    fun pipeline(selection: TextRecognitionPipelineSelection): TextRecognitionPipeline?

    /** Every model [pipeline] needs to read [language], installed or not. */
    fun models(pipeline: TextRecognitionPipeline, language: LanguageTag): List<ModelArtifactDescriptor>

    /** Installs platform-managed models the user approved for [component] reading [language]. */
    suspend fun installPlatformModels(
        component: TextRecognitionComponentId,
        language: LanguageTag,
    ): TextRecognitionPlatformModelsResult
}

sealed interface TextRecognitionPlatformModelsResult {
    data object Installed : TextRecognitionPlatformModelsResult

    data class Failed(
        val reason: String,
    ) : TextRecognitionPlatformModelsResult {
        init {
            require(reason.isNotBlank())
        }
    }
}
