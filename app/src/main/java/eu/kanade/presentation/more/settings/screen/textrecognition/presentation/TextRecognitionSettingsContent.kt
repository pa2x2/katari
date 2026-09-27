package eu.kanade.presentation.more.settings.screen.textrecognition.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import eu.kanade.presentation.components.AppBar
import eu.kanade.presentation.components.AppBarTitle
import eu.kanade.presentation.more.settings.widget.ProfileSpecificChip
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.text.recognition.api.host.TextRecognitionHostActions
import mihon.text.recognition.ui.settings.TextRecognitionSettingsState
import tachiyomi.i18n.*
import tachiyomi.presentation.core.components.ScrollbarLazyColumn
import tachiyomi.presentation.core.components.material.Scaffold
import tachiyomi.presentation.core.components.material.padding
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.time.Duration.Companion.milliseconds

@Composable
internal fun TextRecognitionSettingsContent(
    state: TextRecognitionSettingsState,
    hostActions: TextRecognitionHostActions,
    storedBytes: Long?,
    searchHighlightKey: String?,
    onSearchHighlightConsumed: (String) -> Unit,
    onBack: (() -> Unit)?,
    onChooseEngine: () -> Unit,
    onChooseOverrides: () -> Unit,
    onOpenModels: () -> Unit,
    onChoosePlaygroundLanguage: (LanguageTag) -> Unit,
    onChooseImage: () -> Unit,
    onApprovePlaygroundModels: (List<ModelArtifactDownloadApproval>) -> Unit,
    observeModels: (List<ModelArtifactDescriptor>) -> Flow<Map<ModelArtifactDescriptor, ModelArtifactState>>,
    onSave: () -> Unit,
) {
    val listState = rememberLazyListState()
    val searchTargets = setOf(
        stringResource(MR.strings.text_recognition_settings_playground),
        stringResource(MR.strings.text_recognition_settings_engine),
        stringResource(MR.strings.text_recognition_settings_language_overrides),
        stringResource(MR.strings.model_artifacts_title),
    )
    val highlightPlayground = searchHighlightKey != null && searchHighlightKey in searchTargets
    LaunchedEffect(searchHighlightKey, highlightPlayground) {
        val key = searchHighlightKey ?: return@LaunchedEffect
        if (highlightPlayground) {
            delay(SEARCH_HIGHLIGHT_SCROLL_DELAY)
            listState.animateScrollToItem(PLAYGROUND_ITEM_INDEX)
        }
        onSearchHighlightConsumed(key)
    }

    Scaffold(
        topBar = {
            AppBar(
                titleContent = {
                    AppBarTitle(
                        title = stringResource(MR.strings.text_recognition_title),
                        titleSuffix = { ProfileSpecificChip() },
                    )
                },
                navigateUp = onBack,
                scrollBehavior = it,
            )
        },
    ) { contentPadding ->
        ScrollbarLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(MaterialTheme.padding.medium),
        ) {
            item {
                TextRecognitionPlayground(
                    state = state,
                    hostActions = hostActions,
                    storedBytes = storedBytes,
                    highlighted = highlightPlayground,
                    onChooseEngine = onChooseEngine,
                    onChooseOverrides = onChooseOverrides,
                    onOpenModels = onOpenModels,
                    onChooseLanguage = onChoosePlaygroundLanguage,
                    onChooseImage = onChooseImage,
                    onApproveModels = onApprovePlaygroundModels,
                    observeModels = observeModels,
                    onSave = onSave,
                )
            }
        }
    }
}

private const val PLAYGROUND_ITEM_INDEX = 0
private val SEARCH_HIGHLIGHT_SCROLL_DELAY = 500.milliseconds
