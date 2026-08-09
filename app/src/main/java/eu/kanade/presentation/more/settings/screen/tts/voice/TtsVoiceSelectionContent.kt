package eu.kanade.presentation.more.settings.screen.tts.voice

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import eu.kanade.presentation.more.settings.screen.tts.presentation.TtsPreviewStatus
import mihon.language.api.tag.LanguageTag
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.voice.TtsDefaultVoiceSelection
import mihon.tts.api.voice.TtsVoice
import mihon.tts.api.voice.TtsVoiceId
import mihon.tts.ui.picker.voice.TtsVoicePickerList
import mihon.tts.ui.settings.TtsPreviewState
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TtsVoiceSelectionContent(
    voices: List<TtsVoice>,
    initialSelection: TtsDefaultVoiceSelection?,
    engineDefaultVoice: TtsVoiceId?,
    previewingVoice: TtsVoiceId?,
    networkVoicesAllowed: Boolean,
    language: LanguageTag?,
    contentPadding: PaddingValues,
    onAudition: (TtsVoice) -> Unit,
    onUse: (TtsDefaultVoiceSelection, networkVoiceConfirmed: Boolean) -> Unit,
    onStopPreview: () -> Unit,
    preview: TtsPreviewState,
    onAcknowledgeDisclosure: (TtsProviderDisclosure) -> Unit,
    onOpenSetup: () -> Unit,
) {
    val initialCandidate = remember(voices, initialSelection, engineDefaultVoice) {
        initialSelection?.let { selection ->
            candidateFor(selection, voices, engineDefaultVoice)
        }
    }
    var candidate by remember(initialCandidate) { mutableStateOf(initialCandidate) }
    var pendingNetworkCandidate by remember { mutableStateOf<TtsVoiceCandidate?>(null) }
    var networkConfirmed by remember(networkVoicesAllowed) { mutableStateOf(networkVoicesAllowed) }

    DisposableEffect(Unit) {
        onDispose(onStopPreview)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding),
    ) {
        TtsVoicePickerList(
            voices = voices,
            selected = candidate?.selection,
            previewingVoice = previewingVoice,
            onSelect = { selection, voice ->
                val selectedCandidate = TtsVoiceCandidate(selection, voice)
                if (voice.processing == TtsVoiceProcessing.NetworkRequired && !networkConfirmed) {
                    pendingNetworkCandidate = selectedCandidate
                } else {
                    candidate = selectedCandidate
                    onAudition(voice)
                }
            },
            modifier = Modifier.weight(1f),
            language = language,
            engineDefaultVoice = engineDefaultVoice,
        )
        TtsPreviewStatus(
            preview = preview,
            onAcknowledgeDisclosure = onAcknowledgeDisclosure,
            onOpenSetup = onOpenSetup,
            onRetry = { candidate?.voice?.let(onAudition) },
        )
        Button(
            onClick = {
                candidate?.let { selected ->
                    if (selected.voice.processing == TtsVoiceProcessing.NetworkRequired && !networkConfirmed) {
                        pendingNetworkCandidate = selected
                    } else {
                        onUse(
                            selected.selection,
                            selected.voice.processing == TtsVoiceProcessing.NetworkRequired && networkConfirmed,
                        )
                    }
                }
            },
            enabled = candidate != null,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            Text(stringResource(MR.strings.tts_settings_use_voice))
        }
    }

    pendingNetworkCandidate?.let { pending ->
        AlertDialog(
            onDismissRequest = { pendingNetworkCandidate = null },
            title = { Text(stringResource(MR.strings.tts_settings_network_voice_title)) },
            text = { Text(stringResource(MR.strings.tts_settings_network_voice_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        networkConfirmed = true
                        candidate = pending
                        pendingNetworkCandidate = null
                        onAudition(pending.voice)
                    },
                ) {
                    Text(stringResource(MR.strings.tts_settings_network_voice_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingNetworkCandidate = null }) {
                    Text(stringResource(MR.strings.action_cancel))
                }
            },
        )
    }
}

private data class TtsVoiceCandidate(
    val selection: TtsDefaultVoiceSelection,
    val voice: TtsVoice,
)

private fun candidateFor(
    selection: TtsDefaultVoiceSelection,
    voices: List<TtsVoice>,
    engineDefaultVoice: TtsVoiceId?,
): TtsVoiceCandidate? {
    val voiceId = when (selection) {
        TtsDefaultVoiceSelection.EngineDefault -> engineDefaultVoice
        is TtsDefaultVoiceSelection.Explicit -> selection.voice
    }
    return voices.singleOrNull { it.id == voiceId }?.let { TtsVoiceCandidate(selection, it) }
}
