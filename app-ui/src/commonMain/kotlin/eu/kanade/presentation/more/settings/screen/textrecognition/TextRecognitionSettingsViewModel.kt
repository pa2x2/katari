package eu.kanade.presentation.more.settings.screen.textrecognition

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.language.api.tag.LanguageTag
import app.katari.model.artifacts.api.ModelArtifactStore
import app.katari.text.recognition.api.TextRecognitionFeature
import app.katari.text.recognition.api.host.TextRecognitionHostActions
import app.katari.text.recognition.ui.models.TextRecognitionStoredModels
import app.katari.text.recognition.ui.models.observeStoredModels
import app.katari.text.recognition.ui.settings.TextRecognitionSettingsController
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import eu.kanade.presentation.more.settings.screen.textrecognition.language.LibrarySeriesLanguages
import eu.kanade.presentation.more.settings.screen.textrecognition.language.textRecognitionSuggestedLanguages
import eu.kanade.presentation.more.settings.screen.textrecognition.playground.TextRecognitionPlaygroundImageDecoder
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.storage.DocumentUri
import java.util.Locale

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class TextRecognitionSettingsViewModel(
    feature: TextRecognitionFeature,
    val hostActions: TextRecognitionHostActions,
    modelStore: ModelArtifactStore,
    private val playgroundImages: TextRecognitionPlaygroundImageDecoder,
    preferenceStore: PreferenceStore,
    seriesLanguages: LibrarySeriesLanguages,
) : ViewModel() {
    /** The playground language last chosen, so trying another image does not start from the device language. */
    private val playgroundLanguage = preferenceStore.getString(Preference.appStateKey(PLAYGROUND_LANGUAGE_KEY), "")

    val controller = TextRecognitionSettingsController(
        feature = feature,
        hostActions = hostActions,
        modelStore = modelStore,
        scope = viewModelScope,
        initialPlaygroundLanguage = LanguageTag.parse(playgroundLanguage.get()) ?: deviceLanguage(),
    )
    val state = controller.state

    /** Downloaded model revisions by what they are for, for the storage summary. */
    val storedModels: StateFlow<TextRecognitionStoredModels?> = hostActions.observeStoredModels(modelStore)
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setPlaygroundLanguage(language: LanguageTag) {
        playgroundLanguage.set(language.value)
        controller.setPlaygroundLanguage(language)
    }

    /** Languages the language pickers offer first; empty until the library was read. */
    val suggestedLanguages: StateFlow<List<LanguageTag>> = flow {
        val series = withIOContext { seriesLanguages.languages() }
        emit(textRecognitionSuggestedLanguages(deviceLanguage(), series, hostActions.supportedLanguages))
    }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    /** Recognizes the picked image with the draft configuration. */
    fun tryImage(uri: DocumentUri) {
        viewModelScope.launch {
            val image = withIOContext { playgroundImages.decode(uri) } ?: return@launch
            controller.runPlayground(image.preview, image.recognition)
        }
    }

    private fun deviceLanguage(): LanguageTag? = LanguageTag.parse(Locale.getDefault().toLanguageTag())

    private companion object {
        const val PLAYGROUND_LANGUAGE_KEY = "text_recognition_playground_language"
    }
}
