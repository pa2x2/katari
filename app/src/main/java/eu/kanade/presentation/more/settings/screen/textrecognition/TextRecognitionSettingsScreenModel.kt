package eu.kanade.presentation.more.settings.screen.textrecognition

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import eu.kanade.presentation.more.settings.screen.textrecognition.language.librarySeriesLanguages
import eu.kanade.presentation.more.settings.screen.textrecognition.language.textRecognitionSuggestedLanguages
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mihon.entry.interactions.catalogue.EntryCatalogueFeature
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.ui.models.TextRecognitionStoredModels
import mihon.text.recognition.ui.models.observeStoredModels
import mihon.text.recognition.ui.playground.BitmapTextRecognitionImage
import mihon.text.recognition.ui.settings.TextRecognitionSettingsController
import tachiyomi.core.common.preference.Preference
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.domain.entry.repository.EntryRepository
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

internal class TextRecognitionSettingsScreenModel(
    feature: TextRecognitionFeature = Injekt.get(),
    val hostActions: TextRecognitionHostActions = Injekt.get(),
    modelStore: ModelArtifactStore = Injekt.get(),
    private val application: Application = Injekt.get(),
    preferenceStore: PreferenceStore = Injekt.get(),
    entryRepository: EntryRepository = Injekt.get(),
    catalogue: EntryCatalogueFeature = Injekt.get(),
) : ScreenModel {
    /** The playground language last chosen, so trying another image does not start from the device language. */
    private val playgroundLanguage = preferenceStore.getString(Preference.appStateKey(PLAYGROUND_LANGUAGE_KEY), "")

    val controller = TextRecognitionSettingsController(
        feature = feature,
        hostActions = hostActions,
        modelStore = modelStore,
        scope = screenModelScope,
        initialPlaygroundLanguage = LanguageTag.parse(playgroundLanguage.get()) ?: deviceLanguage(),
    )
    val state = controller.state

    /** Downloaded model revisions by what they are for, for the storage summary. */
    val storedModels: StateFlow<TextRecognitionStoredModels?> = hostActions.observeStoredModels(modelStore)
        .stateIn(screenModelScope, SharingStarted.Eagerly, null)

    fun setPlaygroundLanguage(language: LanguageTag) {
        playgroundLanguage.set(language.value)
        controller.setPlaygroundLanguage(language)
    }

    /** Languages the language pickers offer first; empty until the library was read. */
    val suggestedLanguages: StateFlow<List<LanguageTag>> = flow {
        val series = withIOContext { librarySeriesLanguages(entryRepository, catalogue) }
        emit(textRecognitionSuggestedLanguages(deviceLanguage(), series, hostActions.supportedLanguages))
    }.stateIn(screenModelScope, SharingStarted.Eagerly, emptyList())

    /** Recognizes the picked image with the draft configuration. */
    fun tryImage(uri: Uri) {
        screenModelScope.launch {
            val (bitmap, key) = withIOContext {
                val key = application.contentResolver.openInputStream(uri)?.use(ImageContentKey::sha256)
                    ?: return@withIOContext null
                decodePlaygroundImage(uri) to key
            } ?: return@launch
            controller.runPlayground(bitmap, BitmapTextRecognitionImage(bitmap, key))
        }
    }

    private fun deviceLanguage(): LanguageTag? = LanguageTag.parse(Locale.getDefault().toLanguageTag())

    private fun decodePlaygroundImage(uri: Uri): Bitmap =
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(application.contentResolver, uri)) { decoder, info, _ ->
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            val longest = maxOf(info.size.width, info.size.height)
            if (longest > PLAYGROUND_MAXIMUM_EDGE) {
                val scale = PLAYGROUND_MAXIMUM_EDGE.toFloat() / longest
                decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
            }
        }

    private companion object {
        const val PLAYGROUND_LANGUAGE_KEY = "text_recognition_playground_language"

        /** Keeps a tried image legible for recognition without holding a camera-sized bitmap. */
        const val PLAYGROUND_MAXIMUM_EDGE = 2400
    }
}
