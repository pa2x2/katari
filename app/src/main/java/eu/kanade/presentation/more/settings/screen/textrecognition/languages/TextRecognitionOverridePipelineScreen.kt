package eu.kanade.presentation.more.settings.screen.textrecognition.languages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.more.settings.screen.rememberTextRecognitionSettingsScreenModel
import eu.kanade.presentation.util.Screen
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.ui.approval.ModelArtifactDownloadApprovalDialog
import mihon.text.recognition.api.pipeline.TextRecognitionPipelineSelection
import mihon.text.recognition.ui.language.displayName
import mihon.text.recognition.ui.picker.pipeline.TextRecognitionPipelinePickerList
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.screens.LoadingScreen

/**
 * Chooses how one language is read: automatically, or with a pipeline of its own. The choice becomes part of the draft
 * once confirmed; confirming a pipeline whose models are missing asks to download them first.
 */
internal class TextRecognitionOverridePipelineScreen(
    private val languageTag: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTextRecognitionSettingsScreenModel()
        val language = remember(languageTag) { LanguageTag.require(languageTag) }
        val choices by remember(language) { model.controller.observePipelineChoices(language) }
            .collectAsState(initial = null)
        var approval by remember { mutableStateOf<PipelineDownloadApproval?>(null) }

        fun confirm(selection: TextRecognitionPipelineSelection?) {
            if (selection == null) {
                model.controller.removeDraftOverride(language)
            } else {
                model.controller.setDraftOverride(language, selection)
            }
            navigator.pop()
        }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.text_recognition_settings_pipeline_for, language.displayName()),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            val loaded = choices
            if (loaded == null) {
                LoadingScreen(Modifier.padding(contentPadding))
                return@Scaffold
            }
            var candidate by remember(loaded.current, loaded.currentUnavailable) {
                mutableStateOf(loaded.current.takeUnless { loaded.currentUnavailable })
            }
            val candidateOption = candidate?.let { selection ->
                (loaded.presets + loaded.custom).firstOrNull { it.selection == selection }
            }
            val missingModels = candidateOption?.missingModels.orEmpty()
            Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                TextRecognitionPipelinePickerList(
                    choices = loaded,
                    selected = candidate,
                    onSelect = { candidate = it },
                    onDownload = { approval = PipelineDownloadApproval(it, confirmAfterwards = false) },
                    onCancelDownload = model.controller::cancelDownloads,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        if (missingModels.isEmpty()) {
                            confirm(candidate)
                        } else {
                            approval = PipelineDownloadApproval(missingModels, confirmAfterwards = true)
                        }
                    },
                    enabled = if (candidate == null) loaded.automatic != null else candidateOption?.included == true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(
                        if (missingModels.isEmpty()) {
                            stringResource(
                                MR.strings.text_recognition_settings_use_pipeline_for,
                                language.displayName(),
                            )
                        } else {
                            stringResource(MR.strings.text_recognition_settings_download_and_use)
                        },
                    )
                }
            }
            approval?.let { pending ->
                ModelArtifactDownloadApprovalDialog(
                    artifacts = pending.models,
                    onApprove = { approvals ->
                        approval = null
                        model.controller.download(approvals)
                        if (pending.confirmAfterwards) confirm(candidate)
                    },
                    onDismiss = { approval = null },
                )
            }
        }
    }
}

/** Models waiting for download approval, and whether approving them also confirms the chosen pipeline. */
private data class PipelineDownloadApproval(
    val models: List<ModelArtifactDescriptor>,
    val confirmAfterwards: Boolean,
)
