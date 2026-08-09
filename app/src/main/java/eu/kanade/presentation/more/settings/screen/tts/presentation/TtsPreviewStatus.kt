package eu.kanade.presentation.more.settings.screen.tts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import mihon.tts.api.playback.TtsPlaybackFailureReason
import mihon.tts.api.preparation.TtsPreparation
import mihon.tts.api.preparation.TtsSystemSetupReason
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.ui.settings.TtsPreviewState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
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
                message = stringResource(MR.strings.tts_settings_preview_failed),
                action = stringResource(MR.strings.action_retry),
                onClick = onRetry,
            )
        }
        is TtsPreviewState.Failed -> TtsPreviewAction(
            message = when (preview.reason) {
                TtsPlaybackFailureReason.AudioFocusUnavailable ->
                    stringResource(MR.strings.tts_settings_audio_focus_unavailable)
                TtsPlaybackFailureReason.InvalidReadyTts,
                is TtsPlaybackFailureReason.ProviderFailure,
                null,
                -> preview.message ?: stringResource(MR.strings.tts_settings_provider_failed)
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
    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall)) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = onClick) {
            Text(action)
        }
    }
}
