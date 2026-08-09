package eu.kanade.presentation.more.settings.screen.tts

import androidx.appcompat.app.AppCompatDelegate
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import kotlinx.coroutines.launch
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.TtsFeature
import mihon.tts.api.engine.TtsEngineId
import mihon.tts.api.host.TtsHostActionResult
import mihon.tts.api.host.TtsHostActions
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.api.voice.TtsDefaultVoiceSelection
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.ui.settings.TtsSettingsController
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.util.Locale

internal class TtsSettingsScreenModel(
    feature: TtsFeature = Injekt.get(),
    hostActions: TtsHostActions = Injekt.get(),
) : ScreenModel {
    val controller = TtsSettingsController(
        feature = feature,
        hostActions = hostActions,
        scope = screenModelScope,
        initialPreviewLanguage = effectiveUiLanguage(),
    )
    val state = controller.state

    fun onResume() = controller.refresh()

    fun selectDraftEngine(engine: TtsEngineId) = controller.selectDraftEngine(engine)

    fun setDraftPitch(value: Float) = controller.setDraftPitch(value)

    fun setDraftDefaultVoice(voice: TtsDefaultVoiceSelection) = controller.setDraftDefaultVoice(voice)

    fun setDraftVoiceOverride(language: LanguageTag, voice: TtsVoiceId) =
        controller.setDraftVoiceOverride(language, voice)

    fun removeDraftVoiceOverride(language: LanguageTag) = controller.removeDraftVoiceOverride(language)

    fun toggleConfiguredPreview() = controller.toggleConfiguredPreview()

    fun auditionVoice(voice: TtsVoice) = controller.auditionVoice(voice)

    fun stopPreview() = controller.stopPreview()

    fun resolvedVoice(selection: TtsDefaultVoiceSelection): TtsVoice? = controller.resolvedVoice(selection)

    fun saveProfileChanges() = controller.saveProfileChanges()

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

    override fun onDispose() {
        controller.close()
    }

    private fun perform(
        onComplete: (TtsHostActionResult) -> Unit,
        action: suspend () -> TtsHostActionResult,
    ) {
        screenModelScope.launch {
            val result = action()
            onComplete(result)
            if (result == TtsHostActionResult.Completed) {
                controller.stopPreview()
                controller.refresh()
            }
        }
    }
}

private fun effectiveUiLanguage(): LanguageTag {
    val locale = AppCompatDelegate.getApplicationLocales()[0] ?: Locale.getDefault()
    return LanguageTag.parse(locale.toLanguageTag()) ?: LanguageTag.require("en")
}
