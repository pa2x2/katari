package eu.kanade.presentation.more.settings.screen.tts.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.rememberTtsSettingsScreenModel
import eu.kanade.presentation.more.settings.screen.tts.presentTtsHostActionResult
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal class TtsDefaultVoicePickerScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTtsSettingsScreenModel()
        val state by model.state.collectAsState()

        TtsVoicePickerScaffold(
            title = stringResource(MR.strings.tts_settings_default_voice),
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
            TtsVoiceSelectionContent(
                voices = catalog.voices,
                initialSelection = state.defaultVoice,
                engineDefaultVoice = catalog.defaultVoice,
                previewingVoice = state.previewVoice,
                networkVoicesAllowed = state.allowNetworkVoices,
                language = null,
                contentPadding = contentPadding,
                onAudition = model::auditionVoice,
                onUse = { selection, networkVoiceConfirmed ->
                    model.setDraftDefaultVoice(selection, networkVoiceConfirmed)
                    navigator.pop()
                },
                onStopPreview = model::stopPreview,
                preview = state.preview,
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
}
