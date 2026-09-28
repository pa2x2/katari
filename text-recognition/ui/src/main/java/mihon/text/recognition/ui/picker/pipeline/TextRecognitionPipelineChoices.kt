package mihon.text.recognition.ui.picker.pipeline

import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.configuration.TextRecognitionPipelineResolution
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.pipeline.TextRecognitionPreset
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.ui.settings.isIncluded
import mihon.text.recognition.ui.settings.primary

/** The pipelines one language can be read with under a configuration. */
data class TextRecognitionPipelineChoices(
    /** What the configuration uses when the language has no choice of its own; `null` when a choice is required. */
    val automatic: TextRecognitionAutomaticPipeline?,
    val presets: List<TextRecognitionPipelineOption>,
    /** Detector and recognizer combinations no preset already offers. */
    val custom: List<TextRecognitionPipelineOption>,
    /** The language's current choice, or `null` when it follows the automatic pipeline. */
    val current: TextRecognitionPipelineSelection?,
    /** Whether [current] names a pipeline this build cannot run. */
    val currentUnavailable: Boolean,
)

/** The pipeline [recommendedBy] picks for a language that has no choice of its own. */
data class TextRecognitionAutomaticPipeline(
    val option: TextRecognitionPipelineOption,
    val recommendedBy: KnownTextRecognitionProvider?,
)

/** A pipeline the user can pick for one language, with the models it needs and their current state. */
data class TextRecognitionPipelineOption(
    val selection: TextRecognitionPipelineSelection,
    val pipeline: TextRecognitionPipeline,
    val title: String?,
    val description: String?,
    val components: List<KnownTextRecognitionComponent>,
    /** An engine this build excludes that the pipeline depends on, if any. */
    val excludedBy: KnownTextRecognitionProvider?,
    val models: List<Pair<ModelArtifactDescriptor, ModelArtifactState>>,
) {
    val included: Boolean
        get() = excludedBy == null

    val missingModels: List<ModelArtifactDescriptor>
        get() = models.filter { (_, state) ->
            state !is ModelArtifactState.Installed && state !is ModelArtifactState.Downloading
        }.map { it.first }

    val downloadingModels: List<ModelArtifactDescriptor>
        get() = models.filter { (_, state) -> state is ModelArtifactState.Downloading }.map { it.first }
}

/** Every model any choice for [language] needs, so their states can be observed together. */
internal fun TextRecognitionHostActions.pipelineChoiceModels(language: LanguageTag): List<ModelArtifactDescriptor> =
    (presets(language).map { it.pipeline } + pipelines(language)).flatMap { models(it, language) }.distinct()

/** The choices for [language] under [configuration], with model [states] from [pipelineChoiceModels]. */
internal fun TextRecognitionHostActions.pipelineChoices(
    configuration: TextRecognitionConfiguration,
    language: LanguageTag,
    states: Map<ModelArtifactDescriptor, ModelArtifactState>,
): TextRecognitionPipelineChoices {
    fun option(
        selection: TextRecognitionPipelineSelection,
        pipeline: TextRecognitionPipeline,
        preset: TextRecognitionPreset?,
    ) =
        pipelineOption(selection, pipeline, preset, language, states)

    val presets = presets(language)
    val presetPipelines = presets.mapTo(mutableSetOf()) { it.pipeline }
    val current = configuration.overrides.entries.firstOrNull { it.key.primary == language.primary }?.value
    val withoutChoice = configuration.copy(
        overrides = configuration.overrides.filterKeys { it.primary != language.primary },
    )
    val automatic = (resolve(withoutChoice, language) as? TextRecognitionPipelineResolution.Resolved)?.let { resolved ->
        val preset = resolved.preset
        TextRecognitionAutomaticPipeline(
            option = option(
                selection = preset?.let { TextRecognitionPipelineSelection.Preset(it.id) }
                    ?: TextRecognitionPipelineSelection.Custom(resolved.pipeline),
                pipeline = resolved.pipeline,
                preset = preset,
            ),
            recommendedBy = preset?.let { providers.firstOrNull { provider -> provider.id == it.provider } },
        )
    }
    return TextRecognitionPipelineChoices(
        automatic = automatic,
        presets = presets.map { option(TextRecognitionPipelineSelection.Preset(it.id), it.pipeline, it) },
        custom = pipelines(language)
            .filterNot { it in presetPipelines }
            .map { option(TextRecognitionPipelineSelection.Custom(it), it, null) },
        current = current,
        currentUnavailable = current != null &&
            resolve(configuration, language) is TextRecognitionPipelineResolution.OverrideUnavailable,
    )
}

private fun TextRecognitionHostActions.pipelineOption(
    selection: TextRecognitionPipelineSelection,
    pipeline: TextRecognitionPipeline,
    preset: TextRecognitionPreset?,
    language: LanguageTag,
    states: Map<ModelArtifactDescriptor, ModelArtifactState>,
): TextRecognitionPipelineOption {
    val components = pipeline.components.mapNotNull { id -> knownComponents.firstOrNull { it.id == id } }
    return TextRecognitionPipelineOption(
        selection = selection,
        pipeline = pipeline,
        title = preset?.displayName,
        description = preset?.description,
        components = components,
        excludedBy = components
            .mapNotNull { component -> providers.firstOrNull { it.id == component.provider } }
            .firstOrNull { !it.isIncluded },
        models = models(pipeline, language).map { it to (states[it] ?: ModelArtifactState.NotInstalled) },
    )
}
