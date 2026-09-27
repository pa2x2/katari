package eu.kanade.presentation.more.settings.screen.textrecognition.pipeline

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

internal class TextRecognitionPipelinePickerScreenModel(
    private val language: LanguageTag,
    private val hostActions: TextRecognitionHostActions = Injekt.get(),
    private val modelStore: ModelArtifactStore = Injekt.get(),
) : ScreenModel {
    private val presets = hostActions.presets(language)
    private val customPipelines = hostActions.pipelines(language)
    private val models = (presets.map { it.pipeline } + customPipelines).flatMap(hostActions::models).distinct()

    val state: StateFlow<TextRecognitionPipelinePickerState?> = hostActions.observeSelection(language)
        .flatMapLatest { explicit -> modelStates().map { states -> pickerState(explicit, states) } }
        .stateIn(screenModelScope, SharingStarted.Eagerly, null)

    fun select(selection: TextRecognitionPipelineSelection?) = hostActions.setSelection(language, selection)

    fun download(approvals: List<ModelArtifactDownloadApproval>) = approvals.forEach(modelStore::download)

    fun cancel(models: List<ModelArtifactDescriptor>) = models.forEach(modelStore::cancel)

    private fun modelStates() = if (models.isEmpty()) {
        flowOf(emptyMap())
    } else {
        combine(models.map { model -> modelStore.observe(model).map { model to it } }) { it.toMap() }
    }

    private fun pickerState(
        explicit: TextRecognitionPipelineSelection?,
        states: Map<ModelArtifactDescriptor, ModelArtifactState>,
    ): TextRecognitionPipelinePickerState {
        fun option(
            selection: TextRecognitionPipelineSelection,
            pipeline: TextRecognitionPipeline,
            title: String?,
            description: String?,
        ): TextRecognitionPipelineOption {
            val components = pipeline.components.mapNotNull { id ->
                hostActions.knownComponents.firstOrNull { it.id == id }
            }
            return TextRecognitionPipelineOption(
                selection = selection,
                title = title ?: components.joinToString(" + ") { it.displayName },
                description = description,
                components = components,
                models = hostActions.models(pipeline).map { it to (states[it] ?: ModelArtifactState.NotInstalled) },
            )
        }
        return TextRecognitionPipelinePickerState(
            language = language,
            explicitSelection = explicit,
            defaultSelection = hostActions.defaultSelection(language),
            presets = presets.map { preset ->
                option(
                    selection = TextRecognitionPipelineSelection.Preset(preset.id),
                    pipeline = preset.pipeline,
                    title = preset.displayName,
                    description = preset.description,
                )
            },
            customPipelines = customPipelines.map { pipeline ->
                option(TextRecognitionPipelineSelection.Custom(pipeline), pipeline, title = null, description = null)
            },
        )
    }
}
