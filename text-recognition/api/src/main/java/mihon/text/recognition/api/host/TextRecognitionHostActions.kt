package mihon.text.recognition.api.host

import kotlinx.coroutines.flow.Flow
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPreset

/** Provider-neutral boundary shared by settings and reader surfaces. */
interface TextRecognitionHostActions {
    val knownComponents: List<KnownTextRecognitionComponent>
    val presets: List<TextRecognitionPreset>

    /** Languages that at least one component of this build can read. */
    val supportedLanguages: List<LanguageTag>

    /** The profile's explicit choice for [language], or `null` when the default preset applies. */
    fun observeSelection(language: LanguageTag): Flow<TextRecognitionPipelineSelection?>

    /** The pipeline used for [language] when nothing was chosen explicitly, if this build has one. */
    fun defaultSelection(language: LanguageTag): TextRecognitionPipelineSelection?

    fun setSelection(language: LanguageTag, selection: TextRecognitionPipelineSelection?)

    /** Pipeline a selection resolves to, or `null` when it names an unknown preset. */
    fun resolve(selection: TextRecognitionPipelineSelection): TextRecognitionPipeline?

    /** Every model [pipeline] needs, installed or not. */
    fun models(pipeline: TextRecognitionPipeline): List<ModelArtifactDescriptor>
}
