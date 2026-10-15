package eu.kanade.presentation.more.settings.screen.tts.presentation

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import eu.kanade.presentation.more.settings.widget.preferenceHighlight
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.VolumeUp
import mihon.icons.materialsymbols.rounded.Stop
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.surface.PreviewCard
import mihon.presentation.kit.theme.TextRole
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.ui.settings.TtsPreviewState
import mihon.tts.ui.settings.TtsSettingsState
import mihon.tts.ui.settings.previewSample
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

    PreviewCard(modifier = Modifier.preferenceHighlight()) {
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
        TtsPreviewStatus(
            preview = state.preview,
            onAcknowledgeDisclosure = onAcknowledgeDisclosure,
            onOpenSetup = onOpenSetup,
            onRetry = onTogglePreview,
        )
    }
}
