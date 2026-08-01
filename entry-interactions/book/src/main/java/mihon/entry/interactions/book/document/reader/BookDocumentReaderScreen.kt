@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package mihon.entry.interactions.book.document.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import mihon.entry.interactions.book.reader.BookReaderNavigationRow
import mihon.entry.interactions.book.reader.BookReaderNavigationSheet
import mihon.entry.interactions.book.reader.BookReaderProgress
import mihon.entry.interactions.book.reader.BookReaderScaffold
import mihon.entry.interactions.book.reader.settings.BookReaderSettingsDialog
import mihon.entry.interactions.book.reader.translation.BookReaderTextSelection
import mihon.entry.interactions.book.reader.translation.BookSelectionTranslationController
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewActionsMenu
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.translation.ui.session.TranslationSelectionAnchor
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderChrome
import tachiyomi.presentation.core.i18n.stringResource

@Composable
internal fun BookDocumentReaderScreen(
    state: BookDocumentReaderState,
    translationController: BookSelectionTranslationController?,
    onLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    onTransitionReached: (EntryChapter) -> Unit,
    onTerminalObservation: (EntryChapter, Boolean, Boolean, Boolean) -> Unit,
    onChapterSelected: (EntryChapter) -> Unit,
    onChromeVisibilityChange: (Boolean) -> Unit,
    onNavigationVisibilityChange: (Boolean) -> Unit,
    onSettingsVisibilityChange: (Boolean) -> Unit,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onClose: () -> Unit,
) {
    val readerBackground = MaterialTheme.colorScheme.background
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    val automaticTranslationEnabled = translationController?.effectiveEnabled?.collectAsState()?.value == true
    val textInteraction = remember(translationController, automaticTranslationEnabled, rootPosition) {
        BookDocumentTextInteraction(
            observeSelections = automaticTranslationEnabled,
            rootPositionInWindow = rootPosition,
            onSelection = { selection ->
                when (selection) {
                    is BookDocumentTextSelection.Changed -> translationController?.submitSelection(
                        BookReaderTextSelection(
                            ownerIdentity = selection.ownerIdentity,
                            identity = selection.identity,
                            text = selection.text,
                            anchor = selection.boundsInReaderRoot.let { bounds ->
                                TranslationSelectionAnchor(bounds.left, bounds.top, bounds.right, bounds.bottom)
                            },
                        ),
                    )
                    is BookDocumentTextSelection.Cleared ->
                        translationController?.clearSelection(selection.ownerIdentity)
                }
            },
            isReaderTapBlocked = { translationController?.isTranslationActive() == true },
            onBlockedReaderTap = { translationController?.dismissTranslation() },
            onNonLinkTap = { _, _ ->
                if (translationController?.dismissTranslationOnReaderTap() != true) {
                    onChromeVisibilityChange(!state.chromeVisible)
                }
            },
        )
    }

    BackHandler(enabled = state.chromeVisible) { onChromeVisibilityChange(false) }
    CompositionLocalProvider(LocalBookDocumentTextInteraction provides textInteraction) {
        BookReaderScaffold(
            progress = BookReaderProgress.Percentage((state.totalProgression * 100).toInt().coerceIn(0, 100)),
            progressVisible = state.chromeVisible,
            footerColor = readerBackground,
            translationController = translationController,
            onRootPositionInWindow = { rootPosition = it },
            modifier = Modifier
                .fillMaxSize()
                .background(readerBackground),
            content = {
                BookDocumentEndlessViewer(
                    state = state,
                    onLocation = onLocation,
                    onTransitionReached = onTransitionReached,
                    onTerminalObservation = onTerminalObservation,
                    onExternalLinkClick = onExternalLinkClick,
                    onReaderTap = {
                        if (translationController?.dismissTranslationOnReaderTap() != true) {
                            onChromeVisibilityChange(!state.chromeVisible)
                        }
                    },
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing.only(
                                WindowInsetsSides.Top + WindowInsetsSides.Horizontal,
                            ),
                        ),
                )
            },
            overlay = {
                ReaderChrome(
                    visible = state.chromeVisible,
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        TopAppBar(
                            title = { Text(state.entryTitle, maxLines = 1) },
                            navigationIcon = {
                                IconButton(onClick = onClose) {
                                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(MR.strings.action_back))
                                }
                            },
                            actions = {
                                IconButton(onClick = { onNavigationVisibilityChange(true) }) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ViewList,
                                        stringResource(MR.strings.book_table_of_contents),
                                    )
                                }
                                IconButton(onClick = { onSettingsVisibilityChange(true) }) {
                                    Icon(Icons.Outlined.Settings, stringResource(MR.strings.action_settings))
                                }
                                EntryChildWebViewActionsMenu(
                                    resolution = state.childWebView,
                                    onAction = onChildWebViewAction,
                                )
                            },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                            ),
                        )
                    },
                    bottomBar = { Surface {} },
                )
            },
        )
    }

    BookReaderNavigationSheet(
        visible = state.navigationVisible,
        rows = state.chapters.map { BookReaderNavigationRow(it, it.name) },
        selectedIndex = state.chapters.indexOfFirst { it.id == state.currentChapterId },
        onItemClick = onChapterSelected,
        onDismissRequest = { onNavigationVisibilityChange(false) },
    )
    if (state.settingsVisible) {
        BookReaderSettingsDialog(
            settingsSurfaceId = BookDocumentReaderProcessor.SETTINGS_SURFACE_ID,
            capabilities = BookDocumentReaderProcessor.CAPABILITIES,
            onDismissRequest = { onSettingsVisibilityChange(false) },
            onResetProcessorSettings = {},
            processorTabTitles = emptyList(),
            content = {},
        )
    }
}
