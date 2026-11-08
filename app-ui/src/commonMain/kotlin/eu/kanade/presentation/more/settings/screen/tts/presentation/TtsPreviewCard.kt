package eu.kanade.presentation.more.settings.screen.tts.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import app.katari.icons.materialsymbols.MaterialSymbols
import app.katari.icons.materialsymbols.automirroredrounded.VolumeUp
import app.katari.icons.materialsymbols.rounded.Stop
import app.katari.presentation.kit.button.Button
import app.katari.presentation.kit.button.ButtonStyle
import app.katari.presentation.kit.content.Text
import app.katari.presentation.kit.list.ControlRow
import app.katari.presentation.kit.theme.ContentTone
import app.katari.presentation.kit.theme.TextRole
import app.katari.tts.api.provider.TtsProviderDisclosure
import app.katari.tts.ui.engine.engineExplanation
import app.katari.tts.ui.settings.TtsPreviewState
import app.katari.tts.ui.settings.TtsSettingsState
import app.katari.tts.ui.settings.previewSample
import eu.kanade.presentation.more.settings.widget.preferenceHighlight
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** Speaks a fixed sample with the profile's draft speech settings. */
@Composable
internal fun TtsPreviewCard(
    state: TtsSettingsState,
    configurationReady: Boolean,
    onTogglePreview: () -> Unit,
    onAcknowledgeDisclosure: (TtsProviderDisclosure) -> Unit,
    onOpenSetup: () -> Unit,
) {
    val previewActive = state.preview == TtsPreviewState.Preparing || state.preview == TtsPreviewState.Speaking
    val sample = previewSample(state.resolvedDefaultVoice?.language ?: state.previewLanguage)

    ControlRow(modifier = Modifier.preferenceHighlight()) {
        Text(text = sample.text, role = TextRole.Body)
        Button(
            label = when (state.preview) {
                TtsPreviewState.Preparing -> stringResource(MR.strings.tts_settings_preview_preparing)
                TtsPreviewState.Speaking -> stringResource(MR.strings.tts_settings_preview_speaking)
                else -> stringResource(MR.strings.tts_settings_preview_play)
            },
            onClick = onTogglePreview,
            modifier = Modifier.fillMaxWidth(),
            style = ButtonStyle.Tonal,
            icon = if (previewActive) MaterialSymbols.Rounded.Stop else MaterialSymbols.AutoMirroredRounded.VolumeUp,
            enabled = configurationReady,
        )
        // The engine row only names the engine, so the disabled preview says what keeps it from speaking.
        state.selectedEngineState?.status?.engineExplanation()?.takeUnless { configurationReady }?.let { explanation ->
            Text(text = explanation, role = TextRole.Body, tone = ContentTone.Error)
        }
        TtsPreviewStatus(
            preview = state.preview,
            onAcknowledgeDisclosure = onAcknowledgeDisclosure,
            onOpenSetup = onOpenSetup,
            onRetry = onTogglePreview,
        )
    }
}
