package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.tts.RefreshTtsSettingsOnResume
import eu.kanade.presentation.more.settings.screen.tts.TtsSettingsViewModel
import eu.kanade.presentation.more.settings.screen.tts.engine.TtsEnginePickerScreen
import eu.kanade.presentation.more.settings.screen.tts.presentTtsHostActionResult
import eu.kanade.presentation.more.settings.screen.tts.presentation.TtsSettingsContent
import eu.kanade.presentation.more.settings.screen.tts.presentation.ttsSettingsPreferences
import eu.kanade.presentation.more.settings.screen.tts.voice.TtsDefaultVoicePickerScreen
import eu.kanade.presentation.more.settings.screen.tts.voice.TtsVoiceOverridesScreen
import eu.kanade.presentation.util.LocalBackPress
import eu.kanade.tachiyomi.ui.base.viewmodel.rememberScreenViewModel
import mihon.presentation.kit.feedback.rememberScreenMessages
import tachiyomi.i18n.*

object SettingsTtsScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.tts_title

    @Composable
    override fun getPreferences(): List<Preference> = ttsSettingsPreferences()

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val messages = rememberScreenMessages()
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.current
        val model = rememberTtsSettingsViewModel()
        val state by model.state.collectAsState()

        RefreshTtsSettingsOnResume(model)
        TtsSettingsContent(
            state = state,
            onBack = backPress?.let { { it.invoke() } },
            onChooseEngine = { navigator.push(TtsEnginePickerScreen()) },
            onChooseDefaultVoice = { navigator.push(TtsDefaultVoicePickerScreen()) },
            onChooseVoiceOverrides = { navigator.push(TtsVoiceOverridesScreen()) },
            onPitchChange = model::setDraftPitch,
            onTogglePreview = model::toggleConfiguredPreview,
            onSave = model::saveProfileChanges,
            onDiscard = model::discardProfileChanges,
            configurationReady = model.configurationReady(),
            onAcknowledgeDisclosure = { disclosure ->
                state.selectedEngine?.let { engine ->
                    model.acknowledgeProviderDisclosure(
                        engine,
                        disclosure,
                        { messages.presentTtsHostActionResult(context, it) },
                    )
                }
            },
            onOpenSetup = {
                state.selectedEngine?.let { engine ->
                    model.openSetup(engine, { messages.presentTtsHostActionResult(context, it) })
                }
            },
        )
    }
}

@Composable
internal fun rememberTtsSettingsViewModel(): TtsSettingsViewModel =
    SettingsTtsScreen.rememberScreenViewModel<TtsSettingsViewModel>()
