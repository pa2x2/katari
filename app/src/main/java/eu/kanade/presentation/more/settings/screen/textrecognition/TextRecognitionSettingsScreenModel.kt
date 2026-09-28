package eu.kanade.presentation.more.settings.screen.textrecognition

import android.app.Application
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.ModelArtifactStore
import mihon.text.recognition.api.TextRecognitionFeature
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.api.image.ImageContentKey
import mihon.text.recognition.ui.models.TextRecognitionStoredModels
import mihon.text.recognition.ui.models.observeStoredModels
import mihon.text.recognition.ui.playground.BitmapTextRecognitionImage
import mihon.text.recognition.ui.settings.TextRecognitionSettingsController
import tachiyomi.core.common.util.lang.withIOContext
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

internal class TextRecognitionSettingsScreenModel(
    feature: TextRecognitionFeature = Injekt.get(),
    val hostActions: TextRecognitionHostActions = Injekt.get(),
    modelStore: ModelArtifactStore = Injekt.get(),
    private val application: Application = Injekt.get(),
) : ScreenModel {
    val controller = TextRecognitionSettingsController(
        feature = feature,
        hostActions = hostActions,
        modelStore = modelStore,
        scope = screenModelScope,
        initialPlaygroundLanguage = LanguageTag.parse(Locale.getDefault().toLanguageTag()),
    )
    val state = controller.state

    /** Downloaded model revisions by what they are for, for the storage summary. */
    val storedModels: StateFlow<TextRecognitionStoredModels?> = hostActions.observeStoredModels(modelStore)
        .stateIn(screenModelScope, SharingStarted.Eagerly, null)

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
        /** Keeps a tried image legible for recognition without holding a camera-sized bitmap. */
        const val PLAYGROUND_MAXIMUM_EDGE = 2400
    }
}
