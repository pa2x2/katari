package mihon.translation.ui.picker.engine

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
import mihon.translation.api.engine.TranslationEngineAction
import mihon.translation.api.engine.TranslationEngineId
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/** An engine as a choice in the picker; only a ready engine can be chosen. */
@Composable
internal fun TranslationEngineCard(
    model: TranslationEngineCardModel,
    onSelect: (TranslationEngineId) -> Unit,
    onOpenSetup: (TranslationEngineId) -> Unit,
    onOpenDetails: () -> Unit,
    showManagementActions: Boolean,
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
            TranslationEngineArtwork(engine.artwork, size = KatariTheme.sizes.artwork)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(spacing.xs)) {
                Text(engine.engineName, role = TextRole.SectionTitle)
                Text(engine.details.description, role = TextRole.Body, tone = ContentTone.Secondary)
            }
            RadioButton(selected = model.selected, onClick = null, enabled = model.selectable)
        }
        TranslationEngineStatusPill(model.status)
        model.status.explanation?.let { explanation ->
            Text(explanation.text(), role = TextRole.Body, tone = ContentTone.Secondary)
        }
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(spacing.s),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            if (showManagementActions) {
                model.action?.let { action ->
                    Button(
                        label = action.label(),
                        onClick = { onOpenSetup(engine.id) },
                        style = ButtonStyle.Tonal,
                    )
                }
            }
            Button(
                label = stringResource(MR.strings.translation_engine_details),
                onClick = onOpenDetails,
                style = ButtonStyle.Text,
            )
        }
    }
}

@Composable
private fun TranslationEngineAction.label(): String {
    return stringResource(
        when (this) {
            TranslationEngineAction.Install -> MR.strings.action_install
            TranslationEngineAction.Configure -> MR.strings.translation_settings_configure_provider
            TranslationEngineAction.Setup -> MR.strings.translation_engine_manage
        },
    )
}
