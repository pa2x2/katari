package eu.kanade.presentation.more.settings.screen.textrecognition.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
import mihon.text.recognition.ui.playground.TextRecognitionPlaygroundResult
import mihon.text.recognition.ui.settings.TextRecognitionPlaygroundState
import mihon.text.recognition.ui.settings.TextRecognitionSettingsState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.pluralStringResource
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.DurationUnit

/** Tries the draft configuration on an image the user picks, in the language they choose. */
@Composable
internal fun TextRecognitionPlayground(
    state: TextRecognitionSettingsState,
    hostActions: TextRecognitionHostActions,
    onChooseLanguage: () -> Unit,
    onChooseImage: () -> Unit,
    onApproveModels: (List<ModelArtifactDownloadApproval>) -> Unit,
    onApprovePlatformModels: (TextRecognitionPlaygroundState.PlatformModelsRequired) -> Unit,
    observeModels: (List<ModelArtifactDescriptor>) -> Flow<Map<ModelArtifactDescriptor, ModelArtifactState>>,
) {
    ElevatedCard(
        modifier = Modifier
            .padding(horizontal = MaterialTheme.padding.medium)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
    ) {
        Column(
            modifier = Modifier.padding(MaterialTheme.padding.large),
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            Text(
                text = stringResource(MR.strings.text_recognition_settings_try_it_summary),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(MaterialTheme.padding.small)) {
                FilledTonalButton(
                    onClick = onChooseLanguage,
                    contentPadding = ButtonDefaults.ButtonWithIconContentPadding,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = stringResource(MR.strings.text_recognition_settings_text_language),
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Text(
                        text = state.playgroundLanguage?.displayName().orEmpty(),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = MaterialTheme.padding.small),
                    )
                    Icon(
                        imageVector = Icons.Outlined.ArrowDropDown,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                }
                OutlinedButton(onClick = onChooseImage, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = Icons.Outlined.Image,
                        contentDescription = null,
                        modifier = Modifier.size(ButtonDefaults.IconSize),
                    )
                    Text(
                        text = stringResource(MR.strings.text_recognition_settings_choose_image),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(start = MaterialTheme.padding.small),
                    )
                }
            }
            PlaygroundPipeline(state = state, hostActions = hostActions)
            PlaygroundOutcome(state.playground, observeModels, onApproveModels, onApprovePlatformModels)
        }
    }
}

/**
 * Which pipeline the draft reads the playground language with and, once the image is read, how many regions it
 * found and how long that took.
 */
@Composable
private fun PlaygroundPipeline(
    state: TextRecognitionSettingsState,
    hostActions: TextRecognitionHostActions,
) {
    val language = state.playgroundLanguage ?: return
    val label = hostActions.resolutionLabel(hostActions.resolve(state.draft, language)) ?: return
    val pipeline = stringResource(MR.strings.text_recognition_settings_read_with, label)
    val recognized = state.playground as? TextRecognitionPlaygroundState.Recognized
    Text(
        text = if (recognized == null) {
            pipeline
        } else {
            val regions = recognized.result.regions.size
            listOf(
                pipeline,
                pluralStringResource(MR.plurals.text_recognition_settings_regions, regions, regions),
                stringResource(
                    MR.strings.text_recognition_settings_duration,
                    recognized.duration.toDouble(DurationUnit.SECONDS),
                ),
            ).joinToString(" · ")
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
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
            text = playground.message
                ?.let { stringResource(MR.strings.text_recognition_settings_failed, it) }
                ?: stringResource(MR.strings.text_recognition_settings_failed_unknown),
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
