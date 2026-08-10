package eu.kanade.presentation.more.settings.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.core.model.rememberScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.Preference
import eu.kanade.presentation.more.settings.screen.tts.RefreshTtsSettingsOnResume
import eu.kanade.presentation.more.settings.screen.tts.TtsSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.tts.engine.TtsEnginePickerScreen
import eu.kanade.presentation.more.settings.screen.tts.presentTtsHostActionResult
import eu.kanade.presentation.more.settings.screen.tts.presentation.TtsSettingsContent
import eu.kanade.presentation.more.settings.screen.tts.voice.TtsDefaultVoicePickerScreen
import eu.kanade.presentation.more.settings.screen.tts.voice.TtsVoiceOverridesScreen
import eu.kanade.presentation.util.LocalBackPress
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

object SettingsTtsScreen : SearchableSettings {

    @ReadOnlyComposable
    @Composable
    override fun getTitleRes() = MR.strings.tts_title

    @Composable
    override fun getPreferences(): List<Preference> = listOf(
        Preference.PreferenceGroup(
            title = stringResource(MR.strings.tts_settings_playground),
            preferenceItems = listOf(
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.tts_settings_engine),
                    isProfileSpecific = true,
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.tts_settings_default_voice),
                    isProfileSpecific = true,
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.tts_settings_language_overrides),
                    isProfileSpecific = true,
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.tts_settings_pitch),
                    isProfileSpecific = true,
                ),
                Preference.PreferenceItem.TextPreference(
                    title = stringResource(MR.strings.tts_settings_playground),
                    subtitle = stringResource(MR.strings.tts_settings_playground_summary),
                ),
            ),
        ),
    )

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val backPress = LocalBackPress.current
        val model = rememberTtsSettingsScreenModel()
        val state by model.state.collectAsState()
        val searchHighlightKey = remember { SearchableSettings.highlightKey }

        RefreshTtsSettingsOnResume(model)
        DisposableEffect(searchHighlightKey) {
            onDispose {
                if (SearchableSettings.highlightKey == searchHighlightKey) {
                    SearchableSettings.highlightKey = null
                }
            }
        }
        TtsSettingsContent(
            state = state,
            searchHighlightKey = searchHighlightKey,
            onSearchHighlightConsumed = { key ->
                if (SearchableSettings.highlightKey == key) {
                    SearchableSettings.highlightKey = null
                }
            },
            onBack = backPress?.let { { it.invoke() } },
            onChooseEngine = { navigator.push(TtsEnginePickerScreen()) },
            onChooseDefaultVoice = { navigator.push(TtsDefaultVoicePickerScreen()) },
            onChooseVoiceOverrides = { navigator.push(TtsVoiceOverridesScreen()) },
            onPitchChange = model::setDraftPitch,
            onTogglePreview = model::toggleConfiguredPreview,
            onSave = model::saveProfileChanges,
            configurationReady = model.configurationReady(),
            onAcknowledgeDisclosure = { disclosure ->
                state.selectedEngine?.let { engine ->
                    model.acknowledgeProviderDisclosure(
                        engine,
                        disclosure,
                        context::presentTtsHostActionResult,
                    )
                }
            },
            onOpenSetup = {
                state.selectedEngine?.let { engine ->
                    model.openSetup(engine, context::presentTtsHostActionResult)
                }
            },
        )
    }
}

@Composable
internal fun rememberTtsSettingsScreenModel(): TtsSettingsScreenModel =
    SettingsTtsScreen.rememberScreenModel { TtsSettingsScreenModel() }
