package eu.kanade.presentation.more.settings.screen.tts.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.RecordVoiceOver
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MultiChoiceSegmentedButtonRow
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import mihon.tts.api.provider.TtsParameterRange
import mihon.tts.api.provider.TtsParameterSupport
import mihon.tts.api.provider.TtsProviderDisclosure
import mihon.tts.api.provider.TtsVoiceProcessing
import mihon.tts.api.voice.TtsDefaultVoiceSelection
import mihon.tts.ui.settings.TtsPreviewState
import mihon.tts.ui.settings.TtsSettingsState
import mihon.tts.ui.settings.displayName
import mihon.tts.ui.settings.previewSample
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.pulsingHighlightBackground
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.abs

@Composable
internal fun TtsSettingsPlayground(
    state: TtsSettingsState,
    configurationReady: Boolean,
    highlighted: Boolean,
    onChooseEngine: () -> Unit,
    onChooseDefaultVoice: () -> Unit,
    onChooseVoiceOverrides: () -> Unit,
    onPitchChange: (Float) -> Unit,
    onTogglePreview: () -> Unit,
    onSave: () -> Unit,
    onAcknowledgeDisclosure: (TtsProviderDisclosure) -> Unit,
    onOpenSetup: () -> Unit,
) {
    val previewActive = state.preview == TtsPreviewState.Preparing || state.preview == TtsPreviewState.Speaking
    val previewVoice = state.resolvedDefaultVoice
    val sample = previewSample(previewVoice?.language ?: state.previewLanguage)

    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.padding.medium)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier
                .pulsingHighlightBackground(Unit.takeIf { highlighted })
                .padding(MaterialTheme.padding.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            TtsPlaygroundSelector(
                label = stringResource(MR.strings.tts_settings_engine),
                value = engineSummary(state),
                icon = Icons.Outlined.Settings,
                onClick = onChooseEngine,
            )
            TtsPlaygroundSelector(
                label = stringResource(MR.strings.tts_settings_default_voice),
                value = defaultVoiceSummary(state),
                icon = Icons.Outlined.RecordVoiceOver,
                onClick = onChooseDefaultVoice,
            )
            TtsPlaygroundSelector(
                label = stringResource(MR.strings.tts_settings_language_overrides),
                value = voiceOverridesSummary(state),
                icon = Icons.Outlined.Translate,
                onClick = onChooseVoiceOverrides,
            )

            val pitch = state.selectedEngineState?.capabilities?.pitch as? TtsParameterSupport.Supported
            pitch?.let { support ->
                TtsPitchSetting(
                    value = state.pitch,
                    range = support.range,
                    onChange = onPitchChange,
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.extraSmall)) {
                Text(
                    text = stringResource(MR.strings.tts_settings_preview),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(text = sample.text, style = MaterialTheme.typography.bodyLarge)
            }
            Button(
                onClick = onTogglePreview,
                enabled = configurationReady,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    imageVector = if (previewActive) Icons.Outlined.Stop else Icons.AutoMirrored.Outlined.VolumeUp,
                    contentDescription = null,
                )
                Text(
                    text = when (state.preview) {
                        TtsPreviewState.Preparing -> stringResource(MR.strings.tts_settings_preview_preparing)
                        TtsPreviewState.Speaking -> stringResource(MR.strings.tts_settings_preview_speaking)
                        else -> stringResource(MR.strings.tts_settings_preview_play)
                    },
                    modifier = Modifier.padding(start = MaterialTheme.padding.small),
                )
            }
            TtsPreviewStatus(
                preview = state.preview,
                onAcknowledgeDisclosure = onAcknowledgeDisclosure,
                onOpenSetup = onOpenSetup,
                onRetry = onTogglePreview,
            )
            Button(
                onClick = onSave,
                enabled = state.hasUnsavedProfileChanges && configurationReady,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(MR.strings.action_save))
            }
        }
    }
}

@Composable
private fun TtsPitchSetting(
    value: Float,
    range: TtsParameterRange,
    onChange: (Float) -> Unit,
) {
    val options = listOf(
        PitchOption(
            stringResource(MR.strings.tts_settings_pitch_low),
            (range.default * 0.8f).coerceAtLeast(range.minimum),
        ),
        PitchOption(stringResource(MR.strings.tts_settings_pitch_natural), range.default),
        PitchOption(
            stringResource(MR.strings.tts_settings_pitch_high),
            (range.default * 1.2f).coerceAtMost(range.maximum),
        ),
    ).distinctBy(PitchOption::value)
    val selected = options.minByOrNull { abs(it.value - value) }

    Column(verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
        Text(stringResource(MR.strings.tts_settings_pitch), style = MaterialTheme.typography.bodyMedium)
        MultiChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            options.forEachIndexed { index, option ->
                SegmentedButton(
                    checked = option == selected,
                    onCheckedChange = { onChange(option.value) },
                    shape = SegmentedButtonDefaults.itemShape(index, options.size),
                ) {
                    Text(option.label)
                }
            }
        }
    }
}

@Composable
private fun TtsPlaygroundSelector(
    label: String,
    value: String,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    OutlinedCard(onClick = onClick) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(MaterialTheme.padding.medium),
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(imageVector = icon, contentDescription = null)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                )
                Text(
                    text = value,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }
    }
}

@Composable
private fun engineSummary(state: TtsSettingsState): String {
    if (!state.engineInspection.selectionResolved) {
        return stringResource(MR.strings.tts_settings_engine_status_checking)
    }
    if (state.selectedEngine == null) {
        return stringResource(MR.strings.tts_settings_choose_engine)
    }
    return state.selectedEngineState?.engine?.engineName
        ?: stringResource(MR.strings.tts_settings_engine_unavailable)
}

@Composable
private fun defaultVoiceSummary(state: TtsSettingsState): String {
    val voice = state.resolvedDefaultVoice
    if (voice == null) return stringResource(MR.strings.tts_settings_voice_unavailable)
    return when (state.defaultVoice) {
        TtsDefaultVoiceSelection.EngineDefault -> stringResource(
            MR.strings.tts_settings_voice_engine_default_value,
            voice.language.displayName(),
            voice.processing.label(),
        )
        is TtsDefaultVoiceSelection.Explicit -> stringResource(
            MR.strings.tts_settings_voice_value,
            voice.name,
            voice.language.displayName(),
            voice.processing.label(),
        )
    }
}

@Composable
private fun voiceOverridesSummary(state: TtsSettingsState): String {
    return if (state.voiceOverrides.isEmpty()) {
        stringResource(MR.strings.tts_settings_no_voice_overrides)
    } else {
        stringResource(MR.strings.tts_settings_voice_overrides_count, state.voiceOverrides.size)
    }
}

@Composable
private fun TtsVoiceProcessing.label(): String {
    return stringResource(
        when (this) {
            TtsVoiceProcessing.OnDevice -> MR.strings.tts_settings_voice_on_device
            TtsVoiceProcessing.NetworkRequired -> MR.strings.tts_settings_voice_network
            TtsVoiceProcessing.Unknown -> MR.strings.tts_settings_voice_unknown
        },
    )
}

private data class PitchOption(
    val label: String,
    val value: Float,
)
