package eu.kanade.presentation.more.settings.screen.tts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import app.katari.core.platform.locale.AppLocaleStore
import app.katari.language.api.tag.LanguageTag
import app.katari.tts.api.TtsFeature
import app.katari.tts.api.engine.TtsEngineId
import app.katari.tts.api.host.TtsHostActionResult
import app.katari.tts.api.host.TtsHostActions
import app.katari.tts.api.provider.TtsProviderDisclosure
import app.katari.tts.api.voice.TtsDefaultVoiceSelection
import app.katari.tts.api.voice.TtsVoice
import app.katari.tts.api.voice.TtsVoiceId
import app.katari.tts.ui.settings.TtsSettingsController
import app.katari.ui.di.AppUiScope
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoMap
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.ViewModelKey
import kotlinx.coroutines.launch
import java.util.Locale

@Inject
@ViewModelKey
@ContributesIntoMap(AppUiScope::class)
class TtsSettingsViewModel(
    feature: TtsFeature,
    hostActions: TtsHostActions,
    appLocale: AppLocaleStore,
) : ViewModel() {
    val controller = TtsSettingsController(
        feature = feature,
        hostActions = hostActions,
        scope = viewModelScope,
        initialPreviewLanguage = (appLocale.current ?: Locale.getDefault())
            .let { LanguageTag.parse(it.toLanguageTag()) }
            ?: LanguageTag.require("en"),
    )
    val state = controller.state

    fun onResume() = controller.refresh(forceVoiceRefresh = true)

    fun onStop() = controller.stopPreview()

    fun selectDraftEngine(engine: TtsEngineId) = controller.selectDraftEngine(engine)

    fun setDraftPitch(value: Float) = controller.setDraftPitch(value)

    fun setDraftDefaultVoice(
        voice: TtsDefaultVoiceSelection,
        networkVoiceConfirmed: Boolean,
    ) = controller.setDraftDefaultVoice(voice, networkVoiceConfirmed)

    fun setDraftVoiceOverride(
        language: LanguageTag,
        voice: TtsVoiceId,
        networkVoiceConfirmed: Boolean,
    ) = controller.setDraftVoiceOverride(language, voice, networkVoiceConfirmed)

    fun removeDraftVoiceOverride(language: LanguageTag) = controller.removeDraftVoiceOverride(language)

    fun toggleConfiguredPreview() = controller.toggleConfiguredPreview()

    fun auditionVoice(voice: TtsVoice) = controller.auditionVoice(voice)

    fun stopPreview() = controller.stopPreview()

    fun resolvedVoice(selection: TtsDefaultVoiceSelection): TtsVoice? = controller.resolvedVoice(selection)

    fun saveProfileChanges() = controller.saveProfileChanges()

    fun discardProfileChanges() = controller.discardProfileChanges()

    fun configurationReady(): Boolean = controller.configurationReady()

    fun supportsSetup(engine: TtsEngineId): Boolean = controller.supportsSetup(engine)

    fun openSetup(engine: TtsEngineId, onComplete: (TtsHostActionResult) -> Unit) {
        perform(onComplete) { controller.openSetup(engine) }
    }

    fun installVoiceData(
        engine: TtsEngineId,
        languages: Set<LanguageTag>,
        onComplete: (TtsHostActionResult) -> Unit,
    ) {
        perform(onComplete) { controller.installVoiceData(engine, languages) }
    }

    fun acknowledgeProviderDisclosure(
        engine: TtsEngineId,
        disclosure: TtsProviderDisclosure,
        onComplete: (TtsHostActionResult) -> Unit,
    ) {
        perform(onComplete) { controller.acknowledgeProviderDisclosure(engine, disclosure) }
    }

    override fun onCleared() {
        controller.close()
    }

    private fun perform(
        onComplete: (TtsHostActionResult) -> Unit,
        action: suspend () -> TtsHostActionResult,
    ) {
        viewModelScope.launch {
            val result = action()
            onComplete(result)
            if (result == TtsHostActionResult.Completed) {
                controller.stopPreview()
                controller.refresh(forceVoiceRefresh = true)
            }
        }
    }
}
