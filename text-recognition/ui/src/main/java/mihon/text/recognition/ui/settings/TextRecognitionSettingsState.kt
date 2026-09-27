package mihon.text.recognition.ui.settings

import android.graphics.Bitmap
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.text.recognition.api.component.KnownTextRecognitionComponent
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.provider.KnownTextRecognitionProvider
import mihon.text.recognition.api.provider.TextRecognitionBuildAvailability
import mihon.text.recognition.api.result.TextRecognitionResult

/** Settings of one profile while they are edited; [draft] is only stored when saved. */
data class TextRecognitionSettingsState(
    val providers: List<KnownTextRecognitionProvider>,
    val languages: List<LanguageTag>,
    val draft: TextRecognitionConfiguration,
    val hasUnsavedProfileChanges: Boolean = false,
    val playgroundLanguage: LanguageTag? = null,
    val playground: TextRecognitionPlaygroundState = TextRecognitionPlaygroundState.Idle,
) {
    /** The engine the draft uses: its choice when this build includes it, otherwise the first included engine. */
    val effectiveProvider: KnownTextRecognitionProvider?
        get() = providers.firstOrNull { it.id == draft.provider && it.isIncluded }
            ?: providers.firstOrNull(KnownTextRecognitionProvider::isIncluded)
}

val KnownTextRecognitionProvider.isIncluded: Boolean
    get() = buildAvailability == TextRecognitionBuildAvailability.Included

/** Trying the draft configuration on an image the user picked. */
sealed interface TextRecognitionPlaygroundState {
    data object Idle : TextRecognitionPlaygroundState

    data class Running(val image: Bitmap) : TextRecognitionPlaygroundState

    /** The draft needs [models] for the image's language; they download only after approval. */
    data class ModelsRequired(
        val image: Bitmap,
        val models: List<ModelArtifactDescriptor>,
    ) : TextRecognitionPlaygroundState

    data class Recognized(
        val image: Bitmap,
        val result: TextRecognitionResult,
    ) : TextRecognitionPlaygroundState

    /** The draft cannot read the language: no engine reads it, or an override is unusable. */
    data class Unsupported(val image: Bitmap, val language: LanguageTag) : TextRecognitionPlaygroundState

    data class Failed(val image: Bitmap, val message: String?) : TextRecognitionPlaygroundState
}

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
