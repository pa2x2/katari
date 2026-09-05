package mihon.entry.interactions.book.document.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ViewList
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.dp
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.reader.navigation.BookDocumentNavigationTarget
import mihon.entry.interactions.book.document.reader.navigation.documentNavigationPresentation
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderNavigationBarSettings
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderProgressSettings
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderScreenAliveSettings
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderSettingBindings
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderStatusBarSettings
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderTextSelectionMenuSettings
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderTextSizeSettings
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeSettings
import mihon.entry.interactions.book.document.reader.theme.BookDocumentReaderMaterialTheme
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette
import mihon.entry.interactions.book.reader.BookReaderNavigationSheet
import mihon.entry.interactions.book.reader.BookReaderProgress
import mihon.entry.interactions.book.reader.BookReaderScaffold
import mihon.entry.interactions.book.reader.selection.BookSelectionActionCoordinator
import mihon.entry.interactions.book.reader.settings.BookReaderSettingsDialog
import mihon.entry.interactions.book.reader.speech.BookShortFormSpeechOwner
import mihon.entry.interactions.book.reader.speech.BookShortFormSpeechPhase
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewActionsMenu
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.translation.ui.presentation.TranslationResultSpeechPhase
import mihon.translation.ui.presentation.TranslationResultSpeechState
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderChrome
import tachiyomi.presentation.core.components.reader.ReaderChromeBottomBar
import tachiyomi.presentation.core.components.reader.ReaderChromeBottomBarAction
import tachiyomi.presentation.core.components.reader.ReaderChromeTopBar
import tachiyomi.presentation.core.i18n.stringResource
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
internal fun BookDocumentReaderScreen(
    state: BookDocumentReaderState,
    visualChapterProgression: FloatState,
    settingBindings: BookDocumentReaderSettingBindings,
    selectionCoordinator: BookSelectionActionCoordinator?,
    onLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    onTransitionReached: (EntryChapter) -> Unit,
    onTerminalObservation: (EntryChapter, Boolean, Boolean, Boolean) -> Unit,
    onNavigationSelected: (BookDocumentNavigationTarget) -> Unit,
    onChromeToggle: () -> Unit,
    onChromeHide: () -> Unit,
    onUserScrollStarted: () -> Unit,
    onNavigationVisibilityChange: (Boolean) -> Unit,
    onSettingsVisibilityChange: (Boolean) -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    snackbarHostState: SnackbarHostState,
    onAnchorMissing: (String) -> Unit,
    onInternalLinkClick: (BookDocumentSection<EntryChapter>, mihon.book.api.document.BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onAuxiliaryDismiss: () -> Unit,
    onTranslationPopupBoundsChanged: (Rect?) -> Unit,
    onClose: () -> Unit,
) {
    val themeSetting by settingBindings.themeMode.state.collectAsState()
    val textSizeSetting by settingBindings.textSize.state.collectAsState()
    val showTextSelectionMenuSetting by settingBindings.showTextSelectionMenu.state.collectAsState()
    val showReadingProgressSetting by settingBindings.showReadingProgress.state.collectAsState()
    val readingProgressStyleSetting by settingBindings.readingProgressStyle.state.collectAsState()
    val readerPalette = bookDocumentReaderPalette(themeSetting.effectiveValue)
    val focusManager = LocalFocusManager.current
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    var pendingNavigation by remember { mutableStateOf<BookDocumentNavigationTarget?>(null) }
    val currentOnChromeToggle by rememberUpdatedState(onChromeToggle)
    val currentOnNavigationSelected by rememberUpdatedState(onNavigationSelected)
    val observeSelections = selectionCoordinator?.observeSelections?.collectAsState()?.value == true
    val automaticTranslationEnabled =
        selectionCoordinator?.automaticTranslationEnabled?.collectAsState()?.value == true
    val speechState = selectionCoordinator?.speechState?.collectAsState()?.value
    val activeSelectionSpeech = speechState?.owner as? BookShortFormSpeechOwner.Selection
    val translationSpeechState = when (val owner = speechState?.owner) {
        is BookShortFormSpeechOwner.TranslationResult -> TranslationResultSpeechState(
            activeTarget = owner.target,
            phase = when (speechState.phase) {
                BookShortFormSpeechPhase.Preparing -> TranslationResultSpeechPhase.Preparing
                BookShortFormSpeechPhase.Speaking -> TranslationResultSpeechPhase.Speaking
                BookShortFormSpeechPhase.Idle -> error("Idle speech cannot have an owner")
            },
        )
        is BookShortFormSpeechOwner.Selection,
        null,
        -> TranslationResultSpeechState()
    }
    val textInteraction = remember(
        selectionCoordinator,
        observeSelections,
        automaticTranslationEnabled,
        activeSelectionSpeech?.identity,
        rootPosition,
        showTextSelectionMenuSetting.effectiveValue,
    ) {
        BookDocumentTextInteraction(
            observeSelections = observeSelections,
            rootPositionInWindow = rootPosition,
            onSelection = { selection ->
                when (selection) {
                    is BookDocumentTextSelection.Changed -> selectionCoordinator?.submitSelection(selection)
                    is BookDocumentTextSelection.Cleared ->
                        selectionCoordinator?.clearSelection(selection.ownerIdentity)
                }
            },
            isReaderTapBlocked = { selectionCoordinator?.isTranslationActive() == true },
            onBlockedReaderTap = { selectionCoordinator?.dismissTranslation() },
            onNonLinkTap = { _, _ ->
                if (selectionCoordinator?.dismissTranslationOnReaderTap() != true) {
                    currentOnChromeToggle()
                }
            },
            showTextSelectionMenu = showTextSelectionMenuSetting.effectiveValue,
            selectionActions = if (observeSelections) {
                buildSet {
                    add(BookDocumentSelectionAction.Listen)
                    if (!automaticTranslationEnabled) add(BookDocumentSelectionAction.Translate)
                }
            } else {
                emptySet()
            },
            activeSpeechSelectionIdentity = activeSelectionSpeech?.identity,
            onSelectionAction = { ownerIdentity, selectionIdentity, action ->
                selectionCoordinator?.performAction(ownerIdentity, selectionIdentity, action)
            },
        )
    }

    CompositionLocalProvider(
        LocalBookDocumentTextInteraction provides textInteraction,
        LocalBookDocumentTextScale provides textSizeSetting.effectiveValue / 100f,
        LocalBookDocumentReaderPalette provides readerPalette,
    ) {
        BookDocumentReaderMaterialTheme(
            mode = themeSetting.effectiveValue,
            palette = readerPalette,
        ) {
            BookReaderScaffold(
                progress = if (showReadingProgressSetting.effectiveValue) {
                    BookReaderProgress.Chapter(
                        value = visualChapterProgression,
                        style = readingProgressStyleSetting.effectiveValue,
                        activeColor = readerPalette.accent.copy(alpha = 0.75f),
                        trackColor = readerPalette.surfaceVariant,
                    )
                } else {
                    null
                },
                footerColor = readerPalette.background,
                translationController = selectionCoordinator?.translationController,
                translationSpeechState = translationSpeechState,
                onTranslationSpeechToggle = selectionCoordinator?.let { it::toggleTranslationSpeech },
                onTranslationPopupBoundsChanged = onTranslationPopupBoundsChanged,
                onRootPositionInWindow = { rootPosition = it },
                modifier = Modifier
                    .fillMaxSize()
                    .background(readerPalette.background),
                content = {
                    BookDocumentEndlessViewer(
                        currentChapter = state.window.current,
                        currentChapterId = state.currentChapterId,
                        window = state.window,
                        loadedSections = state.loadedSections,
                        loadStates = state.loadStates,
                        navigationRequest = state.navigationRequest,
                        textSizePercent = textSizeSetting.effectiveValue,
                        onLocation = onLocation,
                        onTransitionReached = onTransitionReached,
                        onTerminalObservation = onTerminalObservation,
                        onAnchorMissing = onAnchorMissing,
                        onInternalLinkClick = onInternalLinkClick,
                        onExternalLinkClick = onExternalLinkClick,
                        onScrollStarted = onChromeHide,
                        onUserScrollStarted = onUserScrollStarted,
                        onReaderTap = {
                            if (selectionCoordinator?.dismissTranslationOnReaderTap() != true) {
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
                overlay = { progressIndicator ->
                    ReaderChrome(
                        visible = state.chromeVisible,
                        modifier = Modifier.fillMaxSize(),
                        persistentBottomContent = progressIndicator,
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
                                ReaderChromeBottomBarAction(onClick = { onNavigationVisibilityChange(true) }) {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.ViewList,
                                        stringResource(MR.strings.book_table_of_contents),
                                    )
                                }
                                ReaderChromeBottomBarAction(onClick = { onSettingsVisibilityChange(true) }) {
                                    Icon(Icons.Outlined.Settings, stringResource(MR.strings.action_settings))
                                }
                            }
                        },
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 56.dp)
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                                ),
                            ),
                    )
                },
            )

            LaunchedEffect(state.navigationVisible, pendingNavigation) {
                val target = pendingNavigation ?: return@LaunchedEffect
                if (state.navigationVisible) return@LaunchedEffect
                withFrameNanos { }
                focusManager.clearFocus(force = true)
                withFrameNanos { }
                pendingNavigation = null
                currentOnNavigationSelected(target)
            }

            if (state.navigationVisible) {
                val navigation = remember(
                    state.navigationPresentation,
                    state.publicationNavigation,
                    state.currentChapterId,
                    state.navigationLocator,
                    state.loadedSections,
                ) { state.documentNavigationPresentation() }
                BookReaderNavigationSheet(
                    visible = true,
                    rows = navigation.rows,
                    selectedIndex = navigation.selectedIndex,
                    onItemClick = { pendingNavigation = it },
                    onDismissRequest = { onNavigationVisibilityChange(false) },
                )
            }
            if (state.settingsVisible) {
                BookReaderSettingsDialog(
                    settingsSurfaceId = BookDocumentReaderProcessor.SETTINGS_SURFACE_ID,
                    capabilities = BookDocumentReaderProcessor.CAPABILITIES,
                    sharedSettingBindings = settingBindings.sharedSettings,
                    onDismissRequest = { onSettingsVisibilityChange(false) },
                    onOpenDefaultSettings = onOpenDefaultSettings,
                    onResetProcessorSettings = {
                        settingBindings.themeMode.clearEntryOverride()
                        settingBindings.textSize.clearEntryOverride()
                        settingBindings.keepScreenAlive.clearEntryOverride()
                        settingBindings.showStatusBar.clearEntryOverride()
                        settingBindings.showNavigationBar.clearEntryOverride()
                        settingBindings.showTextSelectionMenu.clearEntryOverride()
                        settingBindings.showReadingProgress.clearEntryOverride()
                        settingBindings.readingProgressStyle.clearEntryOverride()
                    },
                    processorTabTitles = listOf(
                        androidStringResource(R.string.book_reader_appearance_settings),
                        androidStringResource(R.string.book_reader_behavior_settings),
                    ),
                    content = { page ->
                        when (page) {
                            0 -> {
                                BookDocumentReaderThemeSettings(settingBindings.themeMode)
                                BookDocumentReaderTextSizeSettings(settingBindings.textSize)
                            }
                            1 -> {
                                BookDocumentReaderScreenAliveSettings(settingBindings.keepScreenAlive)
                                BookDocumentReaderTextSelectionMenuSettings(settingBindings.showTextSelectionMenu)
                                BookDocumentReaderStatusBarSettings(settingBindings.showStatusBar)
                                BookDocumentReaderNavigationBarSettings(settingBindings.showNavigationBar)
                                BookDocumentReaderProgressSettings(
                                    showProgressBinding = settingBindings.showReadingProgress,
                                    styleBinding = settingBindings.readingProgressStyle,
                                )
                            }
                        }
                    },
                )
            }
            state.auxiliarySection?.let { section ->
                BookDocumentReferenceSheet(section, onAuxiliaryDismiss, onInternalLinkClick, onExternalLinkClick)
            }
        }
    }
}
