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
import mihon.tts.ui.picker.voice.TtsVoiceOverridesList
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal class TtsVoiceOverridesScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTtsSettingsScreenModel()
        val state by model.state.collectAsState()

        TtsVoicePickerScaffold(
            title = stringResource(MR.strings.tts_settings_language_overrides),
            catalog = state.voiceCatalog,
            onBack = navigator::pop,
            onChooseEngine = {
                navigator.push(eu.kanade.presentation.more.settings.screen.tts.engine.TtsEnginePickerScreen())
            },
            onInstallVoiceData = {
                state.selectedEngine?.let { engine ->
                    model.openSetup(engine, context::presentTtsHostActionResult)
                }
            },
            onRetry = model::onResume,
        ) { catalog, contentPadding ->
            TtsVoiceOverridesList(
                overrides = state.voiceOverrides,
                voices = catalog.voices,
                onEdit = { navigator.push(TtsVoicePickerScreen(it)) },
                onDelete = model::removeDraftVoiceOverride,
                onAdd = { navigator.push(TtsLanguageOverridePickerScreen()) },
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
            )
        }
    }
}
