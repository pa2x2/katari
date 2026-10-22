package eu.kanade.presentation.more.settings.screen.tts.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.rememberTtsSettingsViewModel
import eu.kanade.presentation.more.settings.screen.tts.presentTtsHostActionResult
import eu.kanade.presentation.util.Screen
import mihon.presentation.kit.feedback.rememberScreenMessages
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal class TtsDefaultVoicePickerScreen : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val messages = rememberScreenMessages()
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTtsSettingsViewModel()
        val state by model.state.collectAsState()

        TtsVoicePickerScaffold(
            title = stringResource(MR.strings.tts_settings_default_voice),
            catalog = state.voiceCatalog,
            onBack = navigator::pop,
            onChooseEngine = { navigator.pop() },
            onInstallVoiceData = {
                state.selectedEngine?.let { engine ->
                    model.openSetup(engine, { messages.presentTtsHostActionResult(context, it) })
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
}
