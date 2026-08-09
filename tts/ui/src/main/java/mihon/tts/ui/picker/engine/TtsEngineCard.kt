package mihon.tts.ui.picker.engine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import mihon.tts.api.engine.TtsEngineAction
import mihon.tts.api.engine.TtsEngineId
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TtsEngineCard(
    model: TtsEngineCardModel,
    onSelect: (TtsEngineId) -> Unit,
    onOpenSetup: (TtsEngineId) -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val engine = model.state.engine
    val containerColor = if (model.selected) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f)
    } else {
        MaterialTheme.colorScheme.surface
    }
    val borderColor = if (model.selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    OutlinedCard(
        modifier = modifier
            .fillMaxWidth()
            .selectable(
                selected = model.selected,
                enabled = model.selectable,
                role = Role.RadioButton,
                onClick = { if (!model.selected) onSelect(engine.id) },
            ),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(if (model.selected) 2.dp else 1.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                TtsEngineArtwork(engine.artwork, size = 56.dp)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(engine.engineName, style = MaterialTheme.typography.titleMedium)
                    Text(
                        engine.details.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RadioButton(
                    selected = model.selected,
                    enabled = model.selectable,
                    onClick = if (model.selectable && !model.selected) {
                        { onSelect(engine.id) }
                    } else {
                        null
                    },
                )
            }
            Spacer(Modifier.height(12.dp))
            TtsEngineStatusPill(model.status)
            model.status.explanation?.let { explanation ->
                Spacer(Modifier.height(8.dp))
                Text(
                    explanation,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (model.action != null || model.canManage) {
                    FilledTonalButton(onClick = { onOpenSetup(engine.id) }) {
                        Text((model.action ?: TtsEngineAction.SetupVoiceData).label())
                    }
                }
                TextButton(onClick = onOpenDetails) {
                    Text(stringResource(MR.strings.tts_settings_engine_details))
                }
            }
        }
    }
}

@Composable
internal fun TtsEngineStatusPill(status: TtsEngineStatusPresentation) {
    val label = stringResource(
        when (status.label) {
            TtsEngineStatusLabel.Checking -> MR.strings.tts_settings_engine_status_checking
            TtsEngineStatusLabel.Ready -> MR.strings.tts_settings_engine_status_ready
            TtsEngineStatusLabel.NotInstalled -> MR.strings.tts_settings_engine_status_not_installed
            TtsEngineStatusLabel.SetupRequired -> MR.strings.tts_settings_engine_status_setup_required
            TtsEngineStatusLabel.VoiceDataRequired -> MR.strings.tts_settings_engine_status_voice_data_required
            TtsEngineStatusLabel.Unavailable -> MR.strings.tts_settings_engine_status_unavailable
        },
    )
    val colors = when (status.tone) {
        TtsEngineStatusTone.Ready ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        TtsEngineStatusTone.Checking ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        TtsEngineStatusTone.ActionRequired ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TtsEngineStatusTone.Unavailable ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = Modifier.semantics { stateDescription = label },
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.first,
        contentColor = colors.second,
    ) {
        Text(label, modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp))
    }
}

@Composable
private fun TtsEngineAction.label(): String {
    return stringResource(
        when (this) {
            TtsEngineAction.Install -> MR.strings.action_install
            TtsEngineAction.Configure -> MR.strings.tts_settings_engine_configure
            TtsEngineAction.SetupVoiceData -> MR.strings.tts_settings_engine_manage
        },
    )
}
