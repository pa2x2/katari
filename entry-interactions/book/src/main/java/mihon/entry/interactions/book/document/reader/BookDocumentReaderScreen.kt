package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalFocusManager
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeSettings
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette
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
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.translation.ui.session.TranslationSelectionAnchor
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderChrome
import tachiyomi.presentation.core.components.reader.ReaderChromeBottomBar
import tachiyomi.presentation.core.components.reader.ReaderChromeTopBar
import tachiyomi.presentation.core.i18n.stringResource
import kotlin.math.roundToInt
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
internal fun BookDocumentReaderScreen(
    state: BookDocumentReaderState,
    themeBinding: ViewerSettingBinding<BookDocumentReaderThemeMode>,
    translationController: BookSelectionTranslationController?,
    onLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    onTransitionReached: (EntryChapter) -> Unit,
    onTerminalObservation: (EntryChapter, Boolean, Boolean, Boolean) -> Unit,
    onChapterSelected: (EntryChapter) -> Unit,
    onChromeToggle: () -> Unit,
    onChromeHide: () -> Unit,
    onNavigationVisibilityChange: (Boolean) -> Unit,
    onSettingsVisibilityChange: (Boolean) -> Unit,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onClose: () -> Unit,
) {
    val themeSetting by themeBinding.state.collectAsState()
    val readerPalette = bookDocumentReaderPalette(themeSetting.effectiveValue)
    val focusManager = LocalFocusManager.current
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    var pendingChapterSelection by remember { mutableStateOf<EntryChapter?>(null) }
    val currentOnChromeToggle by rememberUpdatedState(onChromeToggle)
    val currentOnChapterSelected by rememberUpdatedState(onChapterSelected)
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
                    currentOnChromeToggle()
                }
            },
        )
    }

    CompositionLocalProvider(
        LocalBookDocumentTextInteraction provides textInteraction,
        LocalBookDocumentReaderPalette provides readerPalette,
    ) {
        BookReaderScaffold(
            progress = BookReaderProgress.Percentage(
                (state.chapterProgression * 100).roundToInt().coerceIn(0, 100),
            ),
            progressVisible = !state.chromeVisible,
            footerColor = readerPalette.background,
            translationController = translationController,
            onRootPositionInWindow = { rootPosition = it },
            modifier = Modifier
                .fillMaxSize()
                .background(readerPalette.background),
            content = {
                BookDocumentEndlessViewer(
                    state = state,
                    onLocation = onLocation,
                    onTransitionReached = onTransitionReached,
                    onTerminalObservation = onTerminalObservation,
                    onExternalLinkClick = onExternalLinkClick,
                    onScrollStarted = onChromeHide,
                    onReaderTap = {
                        if (translationController?.dismissTranslationOnReaderTap() != true) {
                            currentOnChromeToggle()
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
                        ReaderChromeTopBar(
                            title = state.entryTitle,
                            subtitle = state.window.current.name,
                            navigateUp = onClose,
                            actions = {
                                EntryChildWebViewActionsMenu(
                                    resolution = state.childWebView,
                                    onAction = onChildWebViewAction,
                                )
                            },
                        )
                    },
                    bottomBar = {
                        ReaderChromeBottomBar {
                            IconButton(onClick = { onNavigationVisibilityChange(true) }) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.ViewList,
                                    stringResource(MR.strings.book_table_of_contents),
                                )
                            }
                            IconButton(onClick = { onSettingsVisibilityChange(true) }) {
                                Icon(Icons.Outlined.Settings, stringResource(MR.strings.action_settings))
                            }
                        }
                    },
                )
            },
        )
    }

    LaunchedEffect(state.navigationVisible, pendingChapterSelection) {
        val chapter = pendingChapterSelection ?: return@LaunchedEffect
        if (state.navigationVisible) return@LaunchedEffect
        withFrameNanos { }
        focusManager.clearFocus(force = true)
        withFrameNanos { }
        pendingChapterSelection = null
        currentOnChapterSelected(chapter)
    }

    BookReaderNavigationSheet(
        visible = state.navigationVisible,
        rows = state.chapters.map { BookReaderNavigationRow(it, it.name) },
        selectedIndex = state.chapters.indexOfFirst { it.id == state.currentChapterId },
        onItemClick = { pendingChapterSelection = it },
        onDismissRequest = { onNavigationVisibilityChange(false) },
    )
    if (state.settingsVisible) {
        BookReaderSettingsDialog(
            settingsSurfaceId = BookDocumentReaderProcessor.SETTINGS_SURFACE_ID,
            capabilities = BookDocumentReaderProcessor.CAPABILITIES,
            onDismissRequest = { onSettingsVisibilityChange(false) },
            onResetProcessorSettings = themeBinding::clearEntryOverride,
            processorTabTitles = listOf(androidStringResource(R.string.book_reader_appearance_settings)),
            content = { BookDocumentReaderThemeSettings(themeBinding) },
        )
    }
}
