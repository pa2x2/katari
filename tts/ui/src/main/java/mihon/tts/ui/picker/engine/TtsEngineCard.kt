package mihon.tts.ui.picker.engine

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.control.RadioButton
import mihon.presentation.kit.surface.SelectableCard
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.tts.api.engine.TtsEngineAction
import mihon.tts.api.engine.TtsEngineId
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** An engine as a choice in the picker; only a ready engine can be chosen, but any engine can be set up. */
@Composable
internal fun TtsEngineCard(
    model: TtsEngineCardModel,
    onSelect: (TtsEngineId) -> Unit,
    onOpenSetup: (TtsEngineId) -> Unit,
    onOpenDetails: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = KatariTheme.spacing
    val engine = model.state.engine
    SelectableCard(
        selected = model.selected,
        onClick = { if (!model.selected) onSelect(engine.id) },
        modifier = modifier,
        enabled = model.selectable,
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(spacing.m), verticalAlignment = Alignment.Top) {
            TtsEngineArtwork(engine.artwork, size = KatariTheme.sizes.artwork)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(engine.engineName, role = TextRole.SectionTitle)
                Text(engine.details.description, role = TextRole.Body, tone = ContentTone.Secondary)
            }
            RadioButton(selected = model.selected, onClick = null, enabled = model.selectable)
        }
        TtsEngineStatusPill(model.status)
        model.status.explanation?.let { explanation ->
            Text(explanation, role = TextRole.Body, tone = ContentTone.Secondary)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            if (model.action != null || model.canManage) {
                Button(
                    label = (model.action ?: TtsEngineAction.SetupVoiceData).label(),
                    onClick = { onOpenSetup(engine.id) },
                    style = ButtonStyle.Tonal,
                )
            }
            Button(
                label = stringResource(MR.strings.tts_settings_engine_details),
                onClick = onOpenDetails,
                style = ButtonStyle.Text,
            )
        }
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
