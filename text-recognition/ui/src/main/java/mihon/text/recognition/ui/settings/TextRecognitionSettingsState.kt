package mihon.text.recognition.ui.settings

import android.graphics.Bitmap
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.text.recognition.api.component.TextRecognitionComponentId
import mihon.text.recognition.api.configuration.TextRecognitionConfiguration
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

    /** The draft's engine needs data the platform downloads; [installing] is set while it downloads. */
    data class PlatformModelsRequired(
        val image: Bitmap,
        val component: TextRecognitionComponentId,
        val language: LanguageTag,
        val description: String,
        val approximateSizeBytes: Long?,
        val installing: Boolean = false,
    ) : TextRecognitionPlaygroundState

    data class Recognized(
        val image: Bitmap,
        val result: TextRecognitionResult,
    ) : TextRecognitionPlaygroundState

    /** The draft cannot read the language: no engine reads it, or an override is unusable. */
    data class Unsupported(val image: Bitmap, val language: LanguageTag) : TextRecognitionPlaygroundState

    data class Failed(val image: Bitmap, val message: String?) : TextRecognitionPlaygroundState
}
