package eu.kanade.presentation.more.settings.screen.textrecognition.overrides

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

/** Chooses the pipeline one language uses; the choice becomes part of the draft once confirmed. */
internal class TextRecognitionOverridePipelineScreen(
    private val languageTag: String,
) : Screen() {

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.currentOrThrow
        val model = rememberTextRecognitionSettingsScreenModel()
        val language = remember(languageTag) { LanguageTag.require(languageTag) }
        val state by model.state.collectAsState()
        val options by remember(language) { model.controller.observePipelineOptions(language) }
            .collectAsState(initial = null)
        val current = state.draft.overrides.entries
            .firstOrNull { it.key.value.substringBefore('-') == language.value.substringBefore('-') }
            ?.value
        var candidate by remember(current) { mutableStateOf<TextRecognitionPipelineSelection?>(current) }
        var approving by remember { mutableStateOf<List<ModelArtifactDescriptor>?>(null) }

        Scaffold(
            topBar = {
                AppBar(
                    title = stringResource(MR.strings.text_recognition_settings_pipeline_for, language.displayName()),
                    navigateUp = navigator::pop,
                    scrollBehavior = it,
                )
            },
        ) { contentPadding ->
            val loaded = options
            if (loaded == null) {
                LoadingScreen(Modifier.padding(contentPadding))
                return@Scaffold
            }
            Column(modifier = Modifier.fillMaxSize().padding(contentPadding)) {
                TextRecognitionPipelinePickerList(
                    options = loaded,
                    selected = candidate,
                    onSelect = { candidate = it },
                    onDownload = { approving = it },
                    onCancelDownload = model.controller::cancelDownloads,
                    modifier = Modifier.weight(1f),
                )
                Button(
                    onClick = {
                        candidate?.let { model.controller.setDraftOverride(language, it) }
                        navigator.pop()
                    },
                    enabled = candidate != null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Text(stringResource(MR.strings.text_recognition_settings_use_pipeline))
                }
            }
        }
        approving?.let { models ->
            ModelArtifactDownloadApprovalDialog(
                artifacts = models,
                onApprove = { approvals ->
                    approving = null
                    model.controller.download(approvals)
                },
                onDismiss = { approving = null },
            )
        }
    }
}
