package eu.kanade.presentation.more.settings.screen.tts.voice

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.core.platform.currentPlatformContext
import app.katari.language.api.tag.LanguageTag
import app.katari.presentation.kit.feedback.rememberScreenMessages
import app.katari.tts.api.voice.TtsDefaultVoiceSelection
import app.katari.tts.ui.settings.displayName
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.more.settings.screen.rememberTtsSettingsViewModel
import eu.kanade.presentation.more.settings.screen.tts.presentTtsHostActionResult
import eu.kanade.presentation.util.Screen
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

internal class TtsVoicePickerScreen(
    private val language: LanguageTag,
) : Screen() {

    @Composable
    override fun Content() {
        val context = currentPlatformContext()
        val messages = rememberScreenMessages()
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTtsSettingsViewModel()
        val state by model.state.collectAsState()

        TtsVoicePickerScaffold(
            title = stringResource(MR.strings.tts_settings_voice_for_language, language.displayName()),
            catalog = state.voiceCatalog,
            onBack = navigator::pop,
            onChooseEngine = { navigator.pop() },
            onInstallVoiceData = {
                state.selectedEngine?.let { engine ->
                    model.installVoiceData(engine, setOf(language), {
                        messages.presentTtsHostActionResult(context, it)
                    })
                }
            },
            onRetry = model::onResume,
        ) { catalog, contentPadding ->
            TtsVoiceSelectionContent(
                voices = catalog.voices,
                initialSelection = state.voiceOverrides[language]?.let(TtsDefaultVoiceSelection::Explicit),
                engineDefaultVoice = null,
                previewingVoice = state.previewVoice,
                networkVoicesAllowed = state.allowNetworkVoices,
                language = language,
                contentPadding = contentPadding,
                onAudition = model::auditionVoice,
                onUse = { selection, networkVoiceConfirmed ->
                    val explicit = selection as TtsDefaultVoiceSelection.Explicit
                    model.setDraftVoiceOverride(language, explicit.voice, networkVoiceConfirmed)
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
                        model.installVoiceData(
                            engine,
                            setOf(language),
                            { messages.presentTtsHostActionResult(context, it) },
                        )
                    }
                },
            )
        }
    }
}
