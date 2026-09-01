package mihon.translation.ui.picker.engine

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
import mihon.translation.api.engine.TranslationEngineAction
import mihon.translation.api.engine.TranslationEngineId
import mihon.translation.api.preparation.TranslationSystemSetupReason
import mihon.translation.api.preparation.TranslationUnavailableReason
import mihon.translation.ui.picker.language.displayName
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.i18n.stringResource

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TranslationEngineCard(
    model: TranslationEngineCardModel,
    onSelect: (TranslationEngineId) -> Unit,
    onOpenSetup: (TranslationEngineId) -> Unit,
    onOpenDetails: () -> Unit,
    showManagementActions: Boolean,
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
                onClick = {
                    if (!model.selected) onSelect(engine.id)
                },
            ),
        colors = CardDefaults.outlinedCardColors(containerColor = containerColor),
        border = BorderStroke(if (model.selected) 2.dp else 1.dp, borderColor),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.Top,
            ) {
                TranslationEngineArtwork(
                    artwork = engine.artwork,
                    size = 56.dp,
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        text = engine.engineName,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = engine.details.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RadioButton(
                    selected = model.selected,
                    enabled = model.selectable,
                    onClick = null,
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            TranslationEngineStatusPill(model.status)
            model.status.explanation?.let { explanation ->
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = explanation.text(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (showManagementActions) {
                    model.action?.let { action ->
                        FilledTonalButton(onClick = { onOpenSetup(engine.id) }) {
                            Text(action.label())
                        }
                    }
                }
                TextButton(onClick = onOpenDetails) {
                    Text(stringResource(MR.strings.translation_engine_details))
                }
            }
        }
    }
}

@Composable
internal fun TranslationEngineStatusPill(
    presentation: TranslationEngineStatusPresentation,
    modifier: Modifier = Modifier,
) {
    val label = presentation.label.text()
    val colors = when (presentation.tone) {
        TranslationEngineStatusTone.Ready ->
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        TranslationEngineStatusTone.Checking ->
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        TranslationEngineStatusTone.ActionRequired ->
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        TranslationEngineStatusTone.Unavailable ->
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(
        modifier = modifier.semantics { stateDescription = label },
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.first,
        contentColor = colors.second,
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelLarge,
        )
    }
}

@Composable
private fun TranslationEngineStatusLabel.text(): String {
    return stringResource(
        when (this) {
            TranslationEngineStatusLabel.Checking -> MR.strings.translation_engine_status_checking
            TranslationEngineStatusLabel.Ready -> MR.strings.translation_engine_status_ready
            TranslationEngineStatusLabel.NotInstalled -> MR.strings.translation_engine_status_not_installed
            TranslationEngineStatusLabel.ConfigurationRequired ->
                MR.strings.translation_engine_status_setup_required
            TranslationEngineStatusLabel.ProviderDisclosureRequired ->
                MR.strings.translation_engine_status_consent_required
            TranslationEngineStatusLabel.ModelDownloadRequired ->
                MR.strings.translation_engine_status_language_data_required
            TranslationEngineStatusLabel.SystemSetupRequired ->
                MR.strings.translation_engine_status_system_setup_required
            TranslationEngineStatusLabel.SetupInProgress ->
                MR.strings.translation_engine_status_setting_up
            TranslationEngineStatusLabel.Unavailable -> MR.strings.translation_engine_status_unavailable
        },
    )
}

@Composable
internal fun TranslationEngineStatusExplanation.text(): String {
    return when (this) {
        TranslationEngineStatusExplanation.InstallProvider ->
            stringResource(MR.strings.translation_engine_install_explanation)
        TranslationEngineStatusExplanation.ProviderDisclosure ->
            stringResource(MR.strings.translation_engine_consent_explanation)
        TranslationEngineStatusExplanation.LanguageData ->
            stringResource(MR.strings.translation_engine_model_explanation)
        is TranslationEngineStatusExplanation.ProviderText -> text
        is TranslationEngineStatusExplanation.SystemSetup -> reason.text()
        is TranslationEngineStatusExplanation.Unavailable -> {
            buildReason ?: reason.text()
        }
    }
}

@Composable
private fun TranslationSystemSetupReason.text(): String {
    return when (this) {
        TranslationSystemSetupReason.ServiceDisabled ->
            stringResource(MR.strings.translation_service_disabled)
        TranslationSystemSetupReason.LanguageModelsRequired ->
            stringResource(MR.strings.translation_models_required)
        is TranslationSystemSetupReason.ProviderActionRequired -> description
    }
}

@Composable
private fun TranslationUnavailableReason.text(): String {
    return when (this) {
        is TranslationUnavailableReason.UnsupportedOs ->
            stringResource(MR.strings.translation_unsupported_os, minimumApi)
        TranslationUnavailableReason.ServiceMissing ->
            stringResource(MR.strings.translation_service_missing)
        TranslationUnavailableReason.SystemSettingsUnavailable ->
            stringResource(MR.strings.translation_settings_unavailable)
        is TranslationUnavailableReason.UnsupportedLanguage ->
            stringResource(MR.strings.translation_unsupported_language, language.displayName())
        is TranslationUnavailableReason.UnsupportedLanguagePair ->
            stringResource(
                MR.strings.translation_unsupported_pair,
                source.displayName(),
                target.displayName(),
            )
        is TranslationUnavailableReason.EngineUnavailable ->
            reason
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
