package mihon.text.recognition.ui.settings

import android.graphics.Bitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
import mihon.text.recognition.api.configuration.TextRecognitionPipelineResolution
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.api.host.TextRecognitionPlatformModelsResult
import mihon.text.recognition.api.image.TextRecognitionImage
import mihon.text.recognition.api.pipeline.TextRecognitionPipeline
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.api.preparation.TextRecognitionPreparation
import mihon.text.recognition.api.provider.TextRecognitionProviderId
import mihon.text.recognition.api.request.TextRecognitionRequest
import mihon.text.recognition.api.result.TextRecognitionExecution
import mihon.text.recognition.ui.picker.pipeline.TextRecognitionPipelineChoices
import mihon.text.recognition.ui.picker.pipeline.pipelineChoiceModels
import mihon.text.recognition.ui.picker.pipeline.pipelineChoices

/**
 * Edits one profile's recognition configuration as a draft, lets the user try the draft on an image, and stores it
 * only on [save]. Changes stored elsewhere are adopted while the draft has no unsaved edits.
 */
class TextRecognitionSettingsController(
    private val feature: TextRecognitionFeature,
    private val hostActions: TextRecognitionHostActions,
    private val modelStore: ModelArtifactStore,
    private val scope: CoroutineScope,
    initialPlaygroundLanguage: LanguageTag?,
) {
    private var saved = TextRecognitionConfiguration(provider = null)
    private val mutableState = MutableStateFlow(
        TextRecognitionSettingsState(
            providers = hostActions.providers,
            languages = hostActions.supportedLanguages,
            draft = saved,
            playgroundLanguage = initialPlaygroundLanguage
                ?.takeIf { language -> hostActions.supportedLanguages.any { it.primary == language.primary } }
                ?: hostActions.supportedLanguages.firstOrNull(),
        ),
    )
    val state: StateFlow<TextRecognitionSettingsState> = mutableState.asStateFlow()

    private var playgroundJob: Job? = null
    private var playgroundImage: Pair<Bitmap, TextRecognitionImage>? = null

    init {
        hostActions.observeConfiguration()
            .onEach { configuration ->
                saved = configuration
                mutableState.update { current ->
                    if (current.hasUnsavedProfileChanges) current else current.copy(draft = configuration)
                }
            }
            .launchIn(scope)
    }

    fun selectDraftEngine(provider: TextRecognitionProviderId) = editDraft { it.copy(provider = provider) }

    fun setDraftOverride(language: LanguageTag, selection: TextRecognitionPipelineSelection) = editDraft { draft ->
        draft.copy(overrides = draft.overrides.filterKeys { it.primary != language.primary } + (language to selection))
    }

    fun removeDraftOverride(language: LanguageTag) = editDraft { draft ->
        draft.copy(overrides = draft.overrides.filterKeys { it.primary != language.primary })
    }

    fun save() {
        val draft = mutableState.value.draft
        hostActions.saveConfiguration(draft)
        saved = draft
        mutableState.update { it.copy(hasUnsavedProfileChanges = false) }
    }

    /** Drops the draft and returns to the stored configuration. */
    fun discard() = editDraft { saved }

    /** How the draft reads [language]. */
    fun resolve(language: LanguageTag): TextRecognitionPipelineResolution =
        hostActions.resolve(mutableState.value.draft, language)

    /** How [language] can be read under the draft, with live model states. */
    fun observePipelineChoices(language: LanguageTag): Flow<TextRecognitionPipelineChoices> {
        val configuration = mutableState.value.draft
        return observeModels(hostActions.pipelineChoiceModels(language))
            .map { states -> hostActions.pipelineChoices(configuration, language, states) }
    }

    fun observeModels(models: List<ModelArtifactDescriptor>): Flow<Map<ModelArtifactDescriptor, ModelArtifactState>> =
        if (models.isEmpty()) {
            flowOf(emptyMap())
        } else {
            combine(models.map { model -> modelStore.observe(model).map { model to it } }) { it.toMap() }
        }

    fun download(approvals: List<ModelArtifactDownloadApproval>) = approvals.forEach(modelStore::download)

    fun cancelDownloads(models: List<ModelArtifactDescriptor>) = models.forEach(modelStore::cancel)

    fun setPlaygroundLanguage(language: LanguageTag) {
        mutableState.update { it.copy(playgroundLanguage = language) }
        playgroundImage?.let { (bitmap, image) -> runPlayground(bitmap, image) }
    }

    /** Recognizes [image] with the draft configuration; [bitmap] is what the result is drawn on. */
    fun runPlayground(bitmap: Bitmap, image: TextRecognitionImage) {
        playgroundImage = bitmap to image
        playgroundJob?.cancel()
        val language = mutableState.value.playgroundLanguage ?: return
        playgroundJob = scope.launch {
            setPlayground(TextRecognitionPlaygroundState.Running(bitmap))
            val pipeline = (resolve(language) as? TextRecognitionPipelineResolution.Resolved)?.pipeline
                ?: return@launch setPlayground(TextRecognitionPlaygroundState.Unsupported(bitmap, language))
            try {
                val request = TextRecognitionRequest(image, language, pipeline = pipeline)
                val state = when (val preparation = feature.prepare(request)) {
                    is TextRecognitionPreparation.Ready -> when (
                        val execution = feature.recognize(
                            preparation.recognition,
                        )
                    ) {
                        is TextRecognitionExecution.Success ->
                            TextRecognitionPlaygroundState.Recognized(bitmap, execution.result)
                        is TextRecognitionExecution.PreparationChanged ->
                            TextRecognitionPlaygroundState.Failed(bitmap, null)
                        is TextRecognitionExecution.Failed -> TextRecognitionPlaygroundState.Failed(
                            bitmap,
                            execution.message,
                        )
                    }
                    is TextRecognitionPreparation.ModelsRequired ->
                        TextRecognitionPlaygroundState.ModelsRequired(bitmap, preparation.models)
                    is TextRecognitionPreparation.PlatformModelsRequired ->
                        TextRecognitionPlaygroundState.PlatformModelsRequired(
                            image = bitmap,
                            component = preparation.component,
                            language = preparation.language,
                            description = preparation.description,
                            approximateSizeBytes = preparation.approximateSizeBytes,
                        )
                    else -> TextRecognitionPlaygroundState.Unsupported(bitmap, language)
                }
                setPlayground(state)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                setPlayground(TextRecognitionPlaygroundState.Failed(bitmap, error.message))
            }
        }
    }

    /** Downloads the models the playground needs and tries the image again once they are installed. */
    fun approvePlaygroundModels(approvals: List<ModelArtifactDownloadApproval>) {
        download(approvals)
        val (bitmap, image) = playgroundImage ?: return
        playgroundJob?.cancel()
        playgroundJob = scope.launch {
            observeModels(approvals.map { it.artifact })
                .first { states -> states.values.all { it is ModelArtifactState.Installed } }
            runPlayground(bitmap, image)
        }
    }

    /** Has the platform download the data the user approved, then tries the image again. */
    fun approvePlaygroundPlatformModels(required: TextRecognitionPlaygroundState.PlatformModelsRequired) {
        val (bitmap, image) = playgroundImage ?: return
        playgroundJob?.cancel()
        setPlayground(required.copy(installing = true))
        playgroundJob = scope.launch {
            when (val result = hostActions.installPlatformModels(required.component, required.language)) {
                TextRecognitionPlatformModelsResult.Installed -> runPlayground(bitmap, image)
                is TextRecognitionPlatformModelsResult.Failed ->
                    setPlayground(TextRecognitionPlaygroundState.Failed(bitmap, result.reason))
            }
        }
    }

    private fun setPlayground(playground: TextRecognitionPlaygroundState) {
        mutableState.update { it.copy(playground = playground) }
    }

    /** Applies [edit] and tries the playground image again when the edit changes how its language is read. */
    private fun editDraft(edit: (TextRecognitionConfiguration) -> TextRecognitionConfiguration) {
        val before = mutableState.value
        mutableState.update { current ->
            val draft = edit(current.draft)
            current.copy(draft = draft, hasUnsavedProfileChanges = draft != saved)
        }
        val language = before.playgroundLanguage ?: return
        val (bitmap, image) = playgroundImage ?: return
        if (hostActions.resolve(before.draft, language) != resolve(language)) runPlayground(bitmap, image)
    }
}

internal val LanguageTag.primary: String
    get() = value.substringBefore('-')

/** Components of [pipeline] in pipeline order, for labels. */
fun TextRecognitionHostActions.componentNames(pipeline: TextRecognitionPipeline): String =
    pipeline.components.joinToString(" + ") { id ->
        knownComponents.firstOrNull { it.id == id }?.displayName ?: id.value
    }
