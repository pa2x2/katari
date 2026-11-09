package app.katari.entry.interactions.manga.reader.text.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.katari.core.platform.clipboard.TextClipboard
import app.katari.language.api.tag.LanguageTag
import app.katari.presentation.kit.shortcut.KeyboardShortcutHandler
import eu.kanade.presentation.reader.appbars.ReaderMenuBounds
import eu.kanade.tachiyomi.ui.reader.screen.MangaReaderController

/**
 * The reader's text features over the pages, driven by the reading session's view model and [controller], with their
 * keys ([MangaReaderTextShortcuts]) where the window has keyboard shortcuts.
 */
@Composable
internal fun MangaReaderTextFeatures(
    controller: MangaReaderController,
    menuVisible: Boolean,
    menuBounds: ReaderMenuBounds,
    onChoosePipeline: (LanguageTag) -> Unit,
    onOpenTranslationSettings: () -> Unit,
    openLink: (String) -> Unit,
    clipboard: TextClipboard,
) {
    val viewModel = controller.viewModel
    val textInteraction = controller.textInteraction
    val textState by viewModel.textSession.state.collectAsState()
    val areaResult by textInteraction.areaResult.collectAsState()
    val translationSpeechState by viewModel.textTranslationSpeech.state.collectAsState()
    KeyboardShortcutHandler(MangaReaderTextShortcuts.Translate) { textInteraction.setActive(!textState.active) }
    MangaReaderTextLayer(
        state = textState,
        menuVisible = menuVisible,
        menuBounds = menuBounds,
        translationCoordinator = viewModel.textTranslation.hostCoordinator,
        translateDownloads = viewModel.translateDownloads,
        recognitionLanguages = viewModel.textRecognitionLanguages,
        areaResult = areaResult,
        observeModels = viewModel.textSession::observeModels,
        onConsumeAreaResult = textInteraction::consumeAreaResult,
        onAreaSelected = textInteraction::onAreaSelected,
        onApproveModels = viewModel.textSession::approveModels,
        onApprovePlatformModels = viewModel.textSession::approvePlatformModels,
        onChooseLanguage = viewModel::chooseTextLanguage,
        onChoosePipeline = onChoosePipeline,
        onDismissTranslation = textInteraction::dismissTranslation,
        translationSpeechState = translationSpeechState,
        onTranslationSpeechToggle = viewModel.textTranslationSpeech::toggle,
        onRetryTranslation = viewModel.textSession::resume,
        onToggleOverlay = viewModel::toggleTextTranslationOverlay,
        onToggleOriginal = viewModel.textSession::toggleOriginal,
        onDeleteStoredTranslation = viewModel::deleteStoredTranslation,
        onOpenTranslationSettings = onOpenTranslationSettings,
        openLink = openLink,
        clipboard = clipboard,
        onClose = { textInteraction.setActive(false) },
    )
}
