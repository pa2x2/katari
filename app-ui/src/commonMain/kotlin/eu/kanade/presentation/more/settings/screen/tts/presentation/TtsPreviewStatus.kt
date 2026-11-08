package eu.kanade.presentation.more.settings.screen.tts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.KatariTheme
import app.katari.presentation.kit.theme.TextRole
import app.katari.tts.api.playback.TtsPlaybackFailureReason
import app.katari.tts.api.preparation.TtsPreparation
import app.katari.tts.api.preparation.TtsSystemSetupReason
import app.katari.tts.api.provider.TtsProviderDisclosure
import app.katari.tts.ui.engine.engineExplanation
import app.katari.tts.ui.settings.TtsPreviewState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TtsPreviewStatus(
    preview: TtsPreviewState,
    onAcknowledgeDisclosure: (TtsProviderDisclosure) -> Unit,
    onOpenSetup: () -> Unit,
    onRetry: () -> Unit,
) {
    when (preview) {
        is TtsPreviewState.ActionRequired -> when (val preparation = preview.preparation) {
            is TtsPreparation.ProviderDisclosureRequired -> TtsPreviewAction(
                message = preparation.disclosure.message,
                action = preparation.disclosure.confirmationLabel,
                onClick = { onAcknowledgeDisclosure(preparation.disclosure) },
            )
            is TtsPreparation.SystemSetupRequired -> TtsPreviewAction(
                message = when (val reason = preparation.reason) {
                    TtsSystemSetupReason.ServiceDisabled ->
                        stringResource(MR.strings.tts_settings_engine_status_setup_required)
                    TtsSystemSetupReason.VoiceDataRequired ->
                        stringResource(MR.strings.tts_settings_voice_data_required)
                    is TtsSystemSetupReason.ProviderActionRequired -> reason.description
                },
                action = stringResource(MR.strings.tts_settings_engine_manage),
                onClick = onOpenSetup,
            )
            else -> TtsPreviewAction(
                message = preparation.engineExplanation() ?: stringResource(MR.strings.tts_settings_preview_failed),
                action = stringResource(MR.strings.action_retry),
                onClick = onRetry,
            )
        }
        is TtsPreviewState.Failed -> TtsPreviewAction(
            message = when (val reason = preview.reason) {
                TtsPlaybackFailureReason.AudioFocusUnavailable ->
                    stringResource(MR.strings.tts_settings_audio_focus_unavailable)
                TtsPlaybackFailureReason.InvalidReadyTts,
                is TtsPlaybackFailureReason.ProviderFailure,
                null,
                -> reason?.engineExplanation()
                    ?: preview.message
                    ?: stringResource(MR.strings.tts_settings_provider_failed)
            },
            action = stringResource(MR.strings.action_retry),
            onClick = onRetry,
        )
        TtsPreviewState.Idle,
        TtsPreviewState.Preparing,
        TtsPreviewState.Speaking,
        -> Unit
    }
}

@Composable
private fun TtsPreviewAction(
    message: String,
    action: String,
    onClick: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(KatariTheme.spacing.xs)) {
        Text(text = message, role = TextRole.Body, tone = ContentTone.Error)
        Button(label = action, onClick = onClick, style = ButtonStyle.Text)
    }
}
