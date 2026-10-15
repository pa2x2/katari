package eu.kanade.presentation.more.settings.screen.tts.presentation

import androidx.compose.runtime.Composable
import eu.kanade.presentation.more.settings.PreferenceScreen
import eu.kanade.presentation.more.settings.widget.ProfileSpecificChip
import eu.kanade.presentation.more.settings.widget.draft.SettingsDraftSaveBar
import eu.kanade.presentation.more.settings.widget.draft.rememberSettingsDraftLeaveGuard
import mihon.presentation.kit.navigation.AppBar
import mihon.presentation.kit.navigation.AppBarTitle
import mihon.presentation.kit.navigation.Scaffold
import mihon.tts.api.provider.TtsParameterSupport
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.ui.settings.TtsSettingsState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TtsSettingsContent(
    state: TtsSettingsState,
    onBack: (() -> Unit)?,
    onChooseEngine: () -> Unit,
    onChooseDefaultVoice: () -> Unit,
    onChooseVoiceOverrides: () -> Unit,
    onPitchChange: (Float) -> Unit,
    onTogglePreview: () -> Unit,
    onSave: () -> Unit,
    onDiscard: () -> Unit,
    configurationReady: Boolean,
    onAcknowledgeDisclosure: (TtsProviderDisclosure) -> Unit,
    onOpenSetup: () -> Unit,
) {
    val saveEnabled = state.hasUnsavedProfileChanges && configurationReady
    val navigateUp = rememberSettingsDraftLeaveGuard(
        hasUnsavedChanges = state.hasUnsavedProfileChanges,
        saveEnabled = saveEnabled,
        onSave = onSave,
        onDiscard = onDiscard,
        onLeave = onBack,
    )
    val pitchSupport = state.selectedEngineState?.capabilities?.pitch as? TtsParameterSupport.Supported

    Scaffold(
        topBar = {
            AppBar(
                titleContent = {
                    AppBarTitle(
                        title = stringResource(MR.strings.tts_title),
                        titleSuffix = { ProfileSpecificChip() },
                    )
                },
                navigateUp = onBack?.let { navigateUp },
                scrollBehavior = it,
            )
        },
        bottomBar = {
            SettingsDraftSaveBar(
                visible = state.hasUnsavedProfileChanges,
                saveEnabled = saveEnabled,
                onDiscard = onDiscard,
                onSave = onSave,
            )
        },
    ) { contentPadding ->
        PreferenceScreen(
            items = ttsSettingsPreferences(
                engine = engineSummary(state),
                defaultVoice = defaultVoiceSummary(state),
                voiceOverrides = voiceOverridesSummary(state),
                onChooseEngine = onChooseEngine,
                onChooseDefaultVoice = onChooseDefaultVoice,
                onChooseVoiceOverrides = onChooseVoiceOverrides,
                pitch = pitchSupport?.let { support ->
                    { TtsPitchSetting(value = state.pitch, range = support.range, onChange = onPitchChange) }
                },
                preview = {
                    TtsPreviewCard(
                        state = state,
                        configurationReady = configurationReady,
                        onTogglePreview = onTogglePreview,
                        onAcknowledgeDisclosure = onAcknowledgeDisclosure,
                        onOpenSetup = onOpenSetup,
                    )
                },
            ),
            contentPadding = contentPadding,
        )
    }
}
