package eu.kanade.presentation.more.settings.screen.textrecognition.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.SdStorage
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.coroutines.flow.Flow
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.ui.approval.ModelArtifactDownloadApprovalDialog
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.model.artifacts.ui.state.modelArtifactStateLabel
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.ui.approval.TextRecognitionPlatformModelsDialog
import mihon.text.recognition.ui.language.displayName
import mihon.text.recognition.ui.language.displayNames
import mihon.text.recognition.ui.playground.TextRecognitionPlaygroundResult
import mihon.text.recognition.ui.settings.TextRecognitionPlaygroundState
import mihon.text.recognition.ui.settings.TextRecognitionSettingsState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.components.pulsingHighlightBackground
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun TextRecognitionPlayground(
    state: TextRecognitionSettingsState,
    hostActions: TextRecognitionHostActions,
    storedBytes: Long?,
    highlighted: Boolean,
    onChooseEngine: () -> Unit,
    onChooseOverrides: () -> Unit,
    onOpenModels: () -> Unit,
    onChooseLanguage: () -> Unit,
    onChooseImage: () -> Unit,
    onApproveModels: (List<ModelArtifactDownloadApproval>) -> Unit,
    onApprovePlatformModels: (TextRecognitionPlaygroundState.PlatformModelsRequired) -> Unit,
    observeModels: (List<ModelArtifactDescriptor>) -> Flow<Map<ModelArtifactDescriptor, ModelArtifactState>>,
    onSave: () -> Unit,
) {
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
            PlaygroundSelector(
                label = stringResource(MR.strings.text_recognition_settings_engine),
                value =
                state.effectiveProvider?.name
                    ?: stringResource(MR.strings.text_recognition_engine_status_not_included),
                icon = Icons.Outlined.Settings,
                onClick = onChooseEngine,
            )
            PlaygroundSelector(
                label = stringResource(MR.strings.text_recognition_settings_language_overrides),
                value = if (state.draft.overrides.isEmpty()) {
                    stringResource(MR.strings.text_recognition_settings_no_overrides)
                } else {
                    state.draft.overrides.keys.displayNames()
                },
                icon = Icons.Outlined.Translate,
                onClick = onChooseOverrides,
            )
            PlaygroundSelector(
                label = stringResource(MR.strings.model_artifacts_title),
                value =
                storedBytes?.let { formatModelArtifactSize(it) }
                    ?: stringResource(MR.strings.model_artifacts_summary),
                icon = Icons.Outlined.SdStorage,
                onClick = onOpenModels,
            )
            PlaygroundLanguageSelector(
                state = state,
                hostActions = hostActions,
                onChooseLanguage = onChooseLanguage,
            )
            OutlinedButton(onClick = onChooseImage, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.Image, contentDescription = null)
                Text(
                    text = stringResource(MR.strings.text_recognition_settings_choose_image),
                    modifier = Modifier.padding(start = MaterialTheme.padding.small),
                )
            }
            PlaygroundOutcome(state.playground, observeModels, onApproveModels, onApprovePlatformModels)
            Button(
                onClick = onSave,
                enabled = state.hasUnsavedProfileChanges,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(MR.strings.action_save))
            }
        }
    }
}

@Composable
private fun PlaygroundLanguageSelector(
    state: TextRecognitionSettingsState,
    hostActions: TextRecognitionHostActions,
    onChooseLanguage: () -> Unit,
) {
    val language = state.playgroundLanguage
    val resolution = language?.let { hostActions.resolve(state.draft, it) }
    PlaygroundSelector(
        label = stringResource(MR.strings.text_recognition_settings_text_language),
        value = listOfNotNull(
            language?.displayName(),
            resolution?.let { hostActions.resolutionLabel(it) },
        ).joinToString(" · "),
        icon = Icons.Outlined.Language,
        onClick = onChooseLanguage,
    )
}

@Composable
private fun PlaygroundOutcome(
    playground: TextRecognitionPlaygroundState,
    observeModels: (List<ModelArtifactDescriptor>) -> Flow<Map<ModelArtifactDescriptor, ModelArtifactState>>,
    onApproveModels: (List<ModelArtifactDownloadApproval>) -> Unit,
    onApprovePlatformModels: (TextRecognitionPlaygroundState.PlatformModelsRequired) -> Unit,
) {
    var approving by remember { mutableStateOf<List<ModelArtifactDescriptor>?>(null) }
    var approvingPlatform by remember { mutableStateOf<TextRecognitionPlaygroundState.PlatformModelsRequired?>(null) }
    when (playground) {
        TextRecognitionPlaygroundState.Idle -> Unit
        is TextRecognitionPlaygroundState.Running -> {
            Text(stringResource(MR.strings.text_recognition_settings_recognizing))
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            TextRecognitionPlaygroundResult(image = playground.image, result = null)
        }
        is TextRecognitionPlaygroundState.ModelsRequired -> {
            val states by remember(playground.models) { observeModels(playground.models) }
                .collectAsState(initial = emptyMap())
            playground.models.forEach { model ->
                Text(
                    text = "${model.displayName}: " +
                        modelArtifactStateLabel(model, states[model] ?: ModelArtifactState.NotInstalled),
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            if (states.values.none { it is ModelArtifactState.Downloading }) {
                FilledTonalButton(onClick = { approving = playground.models }) {
                    Text(
                        stringResource(
                            MR.strings.text_recognition_settings_download_models,
                            formatModelArtifactSize(playground.models.sumOf { it.sizeBytes }),
                        ),
                    )
                }
            }
        }
        is TextRecognitionPlaygroundState.PlatformModelsRequired -> {
            Text(
                text = if (playground.installing) {
                    stringResource(MR.strings.reader_text_platform_models_installing)
                } else {
                    playground.description
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (playground.installing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                FilledTonalButton(onClick = { approvingPlatform = playground }) {
                    Text(stringResource(MR.strings.action_download))
                }
            }
        }
        is TextRecognitionPlaygroundState.Recognized -> {
            if (playground.result.regions.isEmpty()) {
                Text(stringResource(MR.strings.text_recognition_settings_no_text))
            }
            TextRecognitionPlaygroundResult(image = playground.image, result = playground.result)
        }
        is TextRecognitionPlaygroundState.Unsupported -> Text(
            text = stringResource(MR.strings.text_recognition_settings_unsupported, playground.language.displayName()),
            color = MaterialTheme.colorScheme.error,
        )
        is TextRecognitionPlaygroundState.Failed -> Text(
            text = stringResource(MR.strings.text_recognition_settings_failed, playground.message.orEmpty()),
            color = MaterialTheme.colorScheme.error,
        )
    }
    approvingPlatform?.let { required ->
        TextRecognitionPlatformModelsDialog(
            description = required.description,
            approximateSizeBytes = required.approximateSizeBytes,
            onApprove = {
                approvingPlatform = null
                onApprovePlatformModels(required)
            },
            onDismiss = { approvingPlatform = null },
        )
    }
    approving?.let { models ->
        ModelArtifactDownloadApprovalDialog(
            artifacts = models,
            onApprove = { approvals ->
                approving = null
                onApproveModels(approvals)
            },
            onDismiss = { approving = null },
        )
    }
}

@Composable
private fun PlaygroundSelector(
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
