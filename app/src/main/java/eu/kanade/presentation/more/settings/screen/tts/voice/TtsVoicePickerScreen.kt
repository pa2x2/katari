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
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.voice.TtsDefaultVoiceSelection
import mihon.tts.ui.settings.displayName
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

internal class TtsVoicePickerScreen(
    private val language: LanguageTag,
) : Screen() {

    @Composable
    override fun Content() {
        val context = LocalContext.current
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTtsSettingsScreenModel()
        val state by model.state.collectAsState()

        TtsVoicePickerScaffold(
            title = stringResource(MR.strings.tts_settings_voice_for_language, language.displayName()),
            catalog = state.voiceCatalog,
            onBack = navigator::pop,
            onChooseEngine = { navigator.pop() },
            onInstallVoiceData = {
                state.selectedEngine?.let { engine ->
                    model.installVoiceData(engine, setOf(language), context::presentTtsHostActionResult)
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
                            context::presentTtsHostActionResult,
                        )
                    }
                },
                onOpenSetup = {
                    state.selectedEngine?.let { engine ->
                        model.installVoiceData(
                            engine,
                            setOf(language),
                            context::presentTtsHostActionResult,
                        )
                    }
                },
            )
        }
    }
}
