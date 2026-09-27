package mihon.text.recognition.runtime.host

import kotlinx.coroutines.flow.Flow
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.runtime.registry.TextRecognitionComponentRegistry
import mihon.text.recognition.runtime.selection.ProfileTextRecognitionPreferences
import mihon.text.recognition.runtime.selection.TextRecognitionPipelineResolver

internal class DefaultTextRecognitionHostActions(
    private val registry: TextRecognitionComponentRegistry,
    private val preferences: ProfileTextRecognitionPreferences,
    private val resolver: TextRecognitionPipelineResolver,
) : TextRecognitionHostActions {
    override val knownComponents: List<KnownTextRecognitionComponent>
        get() = registry.knownComponents

    override val presets: List<TextRecognitionPreset>
        get() = registry.presets

    override val supportedLanguages: List<LanguageTag>
        get() = registry.supportedLanguages

    override fun observeSelection(language: LanguageTag): Flow<TextRecognitionPipelineSelection?> =
        preferences.selection(language).changes()

    override fun defaultSelection(language: LanguageTag): TextRecognitionPipelineSelection? =
        resolver.defaultSelection(language)

    override fun setSelection(language: LanguageTag, selection: TextRecognitionPipelineSelection?) {
        val preference = preferences.selection(language)
        if (selection == null) preference.delete() else preference.set(selection)
    }

    override fun resolve(selection: TextRecognitionPipelineSelection): TextRecognitionPipeline? =
        resolver.pipeline(selection)

    override fun models(pipeline: TextRecognitionPipeline): List<ModelArtifactDescriptor> =
        pipeline.components.mapNotNull(registry::component).flatMap { it.models }.distinct()
}
