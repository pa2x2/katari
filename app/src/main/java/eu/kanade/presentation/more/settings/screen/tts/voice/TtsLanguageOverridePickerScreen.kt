package eu.kanade.presentation.more.settings.screen.tts.voice

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.rememberTtsSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.tts.presentTtsHostActionResult
import eu.kanade.presentation.util.Screen
import mihon.tts.ui.picker.voice.TtsLanguagePickerList
import mihon.tts.ui.settings.ttsLanguageOptions
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal class TtsLanguageOverridePickerScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTtsSettingsScreenModel()
        val state by model.state.collectAsState()

        TtsVoicePickerScaffold(
            title = stringResource(MR.strings.tts_settings_choose_language),
            catalog = state.voiceCatalog,
            onBack = navigator::pop,
            onChooseEngine = { navigator.pop() },
            onInstallVoiceData = {
                state.selectedEngine?.let { engine ->
                    model.openSetup(engine, context::presentTtsHostActionResult)
                }
            },
            onRetry = model::onResume,
        ) { catalog, contentPadding ->
            TtsLanguagePickerList(
                options = ttsLanguageOptions(
                    voices = catalog.voices,
                    excludedLanguages = state.voiceOverrides.keys,
                ),
                selected = null,
                onSelect = { language -> navigator.replace(TtsVoicePickerScreen(language)) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}
