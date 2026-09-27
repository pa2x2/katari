package mihon.entry.interactions.manga.reader.text.ui

import android.graphics.RectF
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.flow.Flow
import mihon.entry.interactions.manga.reader.text.interaction.MangaReaderTextInteraction
import mihon.entry.interactions.manga.reader.text.session.MangaReaderTextBlocker
import mihon.entry.interactions.manga.reader.text.session.MangaReaderTextState
import mihon.language.api.tag.LanguageTag
import mihon.model.artifacts.api.descriptor.ModelArtifactDescriptor
import mihon.model.artifacts.api.download.ModelArtifactDownloadApproval
import mihon.model.artifacts.api.state.ModelArtifactState
import mihon.model.artifacts.ui.approval.ModelArtifactDownloadApprovalDialog
import mihon.text.recognition.ui.approval.TextRecognitionPlatformModelsDialog
import mihon.translation.ui.presentation.CoordinatedTranslationSessionHost
import mihon.translation.ui.session.TranslationSessionHostCoordinator
import tachiyomi.i18n.*

/** Everything translate mode draws above the reader: its toolbar, area outlining, dialogs, and the translation. */
@Composable
internal fun MangaReaderTextLayer(
    state: MangaReaderTextState,
    menuVisible: Boolean,
    translationCoordinator: TranslationSessionHostCoordinator,
    areaResult: MangaReaderTextInteraction.AreaResult?,
    observeModels: (List<ModelArtifactDescriptor>) -> Flow<List<ModelArtifactState>>,
    onConsumeAreaResult: () -> Unit,
    onAreaSelected: (RectF) -> Unit,
    onApproveModels: (List<ModelArtifactDownloadApproval>) -> Unit,
    onApprovePlatformModels: (MangaReaderTextBlocker.PlatformModelsRequired) -> Unit,
    onChooseLanguage: (LanguageTag) -> Unit,
    onOpenSettings: () -> Unit,
    onDismissTranslation: () -> Unit,
    onToggleOverlay: () -> Unit,
    onToggleOriginal: () -> Unit,
    onOpenTranslationSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    var selectingArea by remember { mutableStateOf(false) }
    var approving by remember { mutableStateOf<List<ModelArtifactDescriptor>?>(null) }
    var approvingPlatform by remember { mutableStateOf<MangaReaderTextBlocker.PlatformModelsRequired?>(null) }
    var choosingLanguage by remember { mutableStateOf(false) }

    LaunchedEffect(areaResult) {
        if (areaResult == MangaReaderTextInteraction.AreaResult.NoText) context.toast(MR.strings.reader_text_area_empty)
        if (areaResult != null) onConsumeAreaResult()
    }
    LaunchedEffect(state.active) {
        if (!state.active) selectingArea = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (state.active && selectingArea) {
            MangaReaderAreaSelectionLayer(
                onAreaSelected = { area ->
                    selectingArea = false
                    onAreaSelected(area)
                },
                onCancel = { selectingArea = false },
            )
        } else if (state.active) {
            MangaReaderTextToolbar(
                progress = state.progress,
                overlay = state.overlay,
                showOriginal = state.showOriginal,
                observeModels = observeModels,
                onDownloadModels = { approving = it },
                onDownloadPlatformModels = { approvingPlatform = it },
                onChooseLanguage = { choosingLanguage = true },
                onOpenSettings = onOpenSettings,
                onOpenTranslationSettings = onOpenTranslationSettings,
                onToggleOverlay = onToggleOverlay,
                onToggleOriginal = onToggleOriginal,
                onSelectArea = { selectingArea = true },
                onClose = onClose,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = if (menuVisible) MENU_CLEARANCE else 16.dp),
            )
        }

        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            CoordinatedTranslationSessionHost(
                coordinator = translationCoordinator,
                isTabletUi = maxWidth >= 720.dp,
                modifier = Modifier.fillMaxSize(),
                onDismiss = onDismissTranslation,
            )
        }
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
    approvingPlatform?.let { blocker ->
        TextRecognitionPlatformModelsDialog(
            description = blocker.description,
            approximateSizeBytes = blocker.approximateSizeBytes,
            onApprove = {
                approvingPlatform = null
                onApprovePlatformModels(blocker)
            },
            onDismiss = { approvingPlatform = null },
        )
    }
    val languageBlocker = state.blocker as? MangaReaderTextBlocker.LanguageRequired
    if (choosingLanguage && languageBlocker != null) {
        MangaReaderTextLanguageDialog(
            languages = languageBlocker.languages,
            onChoose = { language ->
                choosingLanguage = false
                onChooseLanguage(language)
            },
            onDismiss = { choosingLanguage = false },
        )
    }
}

/** Space the reader's bottom menu takes, so the toolbar stays visible above it. */
private val MENU_CLEARANCE = 136.dp
