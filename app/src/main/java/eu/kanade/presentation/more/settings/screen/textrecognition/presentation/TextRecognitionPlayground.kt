package eu.kanade.presentation.more.settings.screen.textrecognition.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import kotlinx.coroutines.flow.Flow
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.rounded.Image
import mihon.icons.materialsymbols.rounded.Language
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.ui.approval.ModelArtifactDownloadApprovalDialog
import mihon.model.artifacts.ui.state.formatModelArtifactSize
import mihon.model.artifacts.ui.state.modelArtifactStateLabel
import mihon.presentation.kit.button.Button
import mihon.presentation.kit.button.ButtonStyle
import mihon.presentation.kit.content.Text
import mihon.presentation.kit.progress.LinearProgressIndicator
import mihon.presentation.kit.surface.PreviewCard
import mihon.presentation.kit.theme.ContentTone
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.TextRole
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.ui.approval.TextRecognitionPlatformModelsDialog
import mihon.text.recognition.ui.language.displayName
import mihon.text.recognition.ui.playground.TextRecognitionPlaygroundResult
import mihon.text.recognition.ui.settings.TextRecognitionPlaygroundState
import mihon.text.recognition.ui.settings.TextRecognitionSettingsState
import tachiyomi.i18n.*
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
    val textLanguageLabel = stringResource(MR.strings.text_recognition_settings_text_language)
    PreviewCard {
        Text(
            text = stringResource(MR.strings.text_recognition_settings_try_it_summary),
            role = TextRole.Body,
            tone = ContentTone.Secondary,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(KatariTheme.spacing.s)) {
            Button(
                label = state.playgroundLanguage?.displayName().orEmpty(),
                onClick = onChooseLanguage,
                modifier = Modifier.semantics { contentDescription = textLanguageLabel },
                style = ButtonStyle.Tonal,
                icon = MaterialSymbols.Rounded.Language,
            )
            Button(
                label = stringResource(MR.strings.text_recognition_settings_choose_image),
                onClick = onChooseImage,
                modifier = Modifier.weight(1f),
                style = ButtonStyle.Outlined,
                icon = MaterialSymbols.Rounded.Image,
            )
        }
        PlaygroundPipeline(state = state, hostActions = hostActions)
        PlaygroundOutcome(state.playground, observeModels, onApproveModels, onApprovePlatformModels)
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
        role = TextRole.Supporting,
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
                    role = TextRole.Body,
                )
            }
            if (states.values.none { it is ModelArtifactState.Downloading }) {
                Button(
                    label = stringResource(
                        MR.strings.text_recognition_settings_download_models,
                        formatModelArtifactSize(playground.models.sumOf { it.sizeBytes }),
                    ),
                    onClick = { approving = playground.models },
                    style = ButtonStyle.Tonal,
                )
            }
        }
        is TextRecognitionPlaygroundState.PlatformModelsRequired -> {
            Text(
                text = if (playground.installing) {
                    stringResource(MR.strings.reader_text_platform_models_installing)
                } else {
                    playground.description
                },
                role = TextRole.Body,
            )
            if (playground.installing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            } else {
                Button(
                    label = stringResource(MR.strings.action_download),
                    onClick = { approvingPlatform = playground },
                    style = ButtonStyle.Tonal,
                )
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
            tone = ContentTone.Error,
        )
        is TextRecognitionPlaygroundState.Failed -> Text(
            text = playground.message
                ?.let { stringResource(MR.strings.text_recognition_settings_failed, it) }
                ?: stringResource(MR.strings.text_recognition_settings_failed_unknown),
            tone = ContentTone.Error,
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
