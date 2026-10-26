package mihon.entry.interactions.book.document.reader

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.FloatState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.clearAndSetSemantics
import mihon.core.metro.metroGraph
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.reader.font.BookDocumentReaderFonts
import mihon.entry.interactions.book.document.reader.navigation.BookDocumentJumpHistory
import mihon.entry.interactions.book.document.reader.navigation.BookDocumentNavigationTarget
import mihon.entry.interactions.book.document.reader.navigation.BookDocumentSeekControls
import mihon.entry.interactions.book.document.reader.navigation.BookDocumentSeekState
import mihon.entry.interactions.book.document.reader.navigation.documentNavigationPresentation
import mihon.entry.interactions.book.document.reader.search.BookDocumentBookSearch
import mihon.entry.interactions.book.document.reader.search.BookDocumentBookSearchSheet
import mihon.entry.interactions.book.document.reader.search.BookDocumentChapterSearch
import mihon.entry.interactions.book.document.reader.search.BookDocumentChapterSearchMatching
import mihon.entry.interactions.book.document.reader.search.BookDocumentSearchMatch
import mihon.entry.interactions.book.document.reader.search.LocalBookDocumentSearchMarks
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderAppearancePage
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderBehaviorPage
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderSettingBindings
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReadingModeShortcut
import mihon.entry.interactions.book.document.reader.theme.BookDocumentAuthoredStyling
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentAuthoredStyling
import mihon.entry.interactions.book.document.reader.theme.LocalBookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.theme.bookDocumentReaderPalette
import mihon.entry.interactions.book.document.reader.transition.LocalBookDocumentChapterGroupTitles
import mihon.entry.interactions.book.document.reader.transition.LocalBookDocumentChapterTransitionMode
import mihon.entry.interactions.book.reader.BookReaderProgress
import mihon.entry.interactions.book.reader.BookReaderScaffold
import mihon.entry.interactions.book.reader.selection.BookSelectionActionCoordinator
import mihon.entry.interactions.book.reader.speech.BookShortFormSpeechOwner
import mihon.entry.interactions.book.reader.speech.BookShortFormSpeechPhase
import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationSettingsProvider
import mihon.entry.interactions.book.reader.translation.bookTranslationLanguageDetails
import mihon.entry.interactions.reader.navigation.EntryReaderNavigationSheet
import mihon.entry.interactions.reader.settings.BookDocumentReaderTheme
import mihon.entry.interactions.reader.settings.BookDocumentReadingMode
import mihon.entry.interactions.reader.settings.ChapterTransitionMode
import mihon.entry.interactions.runtime.EntryInteractionActivityGraph
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.viewer.chrome.ui.ReaderChrome
import mihon.entry.viewer.chrome.ui.ReaderChromeBottomBar
import mihon.entry.viewer.chrome.ui.ReaderChromeBottomBarAction
import mihon.entry.viewer.settings.ui.ReaderSettingsDialogHost
import mihon.icons.materialsymbols.MaterialSymbols
import mihon.icons.materialsymbols.automirroredrounded.ViewList
import mihon.icons.materialsymbols.rounded.Settings
import mihon.presentation.kit.feedback.SnackbarHost
import mihon.presentation.kit.feedback.SnackbarHostState
import mihon.presentation.kit.theme.KatariTheme
import mihon.presentation.kit.theme.ReadingPaletteTheme
import mihon.translation.ui.presentation.TranslationResultSpeechPhase
import mihon.translation.ui.presentation.TranslationResultSpeechState
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource
import tachiyomi.presentation.core.util.collectAsState
import androidx.compose.ui.res.stringResource as androidStringResource

@Composable
internal fun BookDocumentReaderScreen(
    state: BookDocumentReaderState,
    visualChapterProgression: FloatState,
    settingBindings: BookDocumentReaderSettingBindings,
    fonts: BookDocumentReaderFonts,
    bookSearch: BookDocumentBookSearch,
    selectionCoordinator: BookSelectionActionCoordinator?,
    onLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    onViewportLocation: (BookDocumentViewerLocation<EntryChapter>) -> Unit,
    jumpHistory: BookDocumentJumpHistory,
    onReturn: () -> Unit,
    onChapterBoundaryReached: (EntryChapter) -> Unit,
    onTransitionRetry: (EntryChapter) -> Unit,
    onChapterEndObservation: (EntryChapter, Boolean, Boolean) -> Unit,
    onNavigationSelected: (BookDocumentNavigationTarget) -> Unit,
    onRevealSearchMatch: (BookDocumentSearchMatch) -> Unit,
    onSearchClosed: (returnTo: BookDocumentNavigationTarget?) -> Unit,
    onChromeToggle: () -> Unit,
    onChromeHide: () -> Unit,
    onUserScrollStarted: () -> Unit,
    onNavigationVisibilityChange: (Boolean) -> Unit,
    onSettingsVisibilityChange: (Boolean) -> Unit,
    onOpenDefaultSettings: () -> Unit,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    onToggleBookmark: () -> Unit,
    snackbarHostState: SnackbarHostState,
    onAnchorMissing: (String) -> Unit,
    onInternalLinkClick: (BookDocumentSection<EntryChapter>, mihon.book.api.document.BookDocumentLinkTarget) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    onAuxiliaryDismiss: () -> Unit,
    onChapterSelectionRetry: () -> Unit,
    onChapterSelectionDismiss: () -> Unit,
    onTranslationPopupBoundsChanged: (Rect?) -> Unit,
    onClose: () -> Unit,
) {
    val seekState = remember { BookDocumentSeekState() }
    SideEffect { seekState.setVisible(state.chromeVisible) }
    val chapterTransitionModeSetting by settingBindings.chapterTransitionMode.state.collectAsState()
    val themeSetting by settingBindings.theme.state.collectAsState()
    val publisherStylesSetting by settingBindings.publisherStyles.state.collectAsState()
    val textSizeSetting by settingBindings.textSize.state.collectAsState()
    val fontSetting by settingBindings.font.state.collectAsState()
    val lineSpacingSetting by settingBindings.lineSpacing.state.collectAsState()
    val marginsSetting by settingBindings.margins.state.collectAsState()
    val textAlignmentSetting by settingBindings.textAlignment.state.collectAsState()
    val showTextSelectionMenuSetting by settingBindings.showTextSelectionMenu.state.collectAsState()
    val showReadingProgressSetting by settingBindings.showReadingProgress.state.collectAsState()
    val showTimeAndBatterySetting by settingBindings.showTimeAndBattery.state.collectAsState()
    val showStatusBarSetting by settingBindings.showStatusBar.state.collectAsState()
    val readingModeSetting by settingBindings.readingMode.state.collectAsState()
    // Paged modes share the same pager and pages. Keep its last observation when only
    // the paging direction changes, because the visible page need not emit again.
    var pageProgress by remember(readingModeSetting.effectiveValue == BookDocumentReadingMode.SCROLL) {
        mutableStateOf<BookReaderProgress.Page?>(BookReaderProgress.Page(1, 1))
    }
    val readingProgressStyleSetting by settingBindings.readingProgressStyle.state.collectAsState()
    val readerPalette = bookDocumentReaderPalette(themeSetting.effectiveValue)
    val authoredStyling = remember(readerPalette, publisherStylesSetting.effectiveValue) {
        BookDocumentAuthoredStyling(readerPalette, publisherStylesSetting.effectiveValue)
    }
    val focusManager = LocalFocusManager.current
    var rootPosition by remember { mutableStateOf(Offset.Zero) }
    var pendingNavigation by remember { mutableStateOf<BookDocumentNavigationTarget?>(null) }
    val search = remember { BookDocumentChapterSearch() }
    val searching = search.query != null
    var bookSearchVisible by remember { mutableStateOf(false) }
    val closeSearch = {
        bookSearchVisible = false
        onSearchClosed(search.close())
    }

    // The settings dialog is a window over the reader, so translation announcements go to it while it is open.
    val settingsSnackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    val sharedSettingsRegistry = remember {
        context.metroGraph<EntryInteractionActivityGraph>().readerSharedSettingsRegistry
    }
    val translationSnackbarHostState = if (state.settingsVisible) settingsSnackbarHostState else snackbarHostState
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
        LocalBookDocumentTypography provides BookDocumentTypography(
            textScale = textSizeSetting.effectiveValue / 100f,
            lineSpacing = lineSpacingSetting.effectiveValue,
            margins = marginsSetting.effectiveValue,
            alignment = textAlignmentSetting.effectiveValue,
            font = fonts.family(fontSetting.effectiveValue),
        ),
        LocalBookDocumentReaderPalette provides readerPalette,
        LocalBookDocumentAuthoredStyling provides authoredStyling,
        LocalBookDocumentChapterTransitionMode provides chapterTransitionModeSetting.effectiveValue,
        LocalBookDocumentChapterGroupTitles provides state.chapterGroupTitles,
        LocalBookDocumentSearchMarks provides search.marks,
    ) {
        ReadingPaletteTheme(
            palette = readerPalette.takeUnless { themeSetting.effectiveValue == BookDocumentReaderTheme.App },
        ) {
            BookReaderScaffold(
                progress = if (showReadingProgressSetting.effectiveValue) {
                    if (readingModeSetting.effectiveValue !=
                        BookDocumentReadingMode.SCROLL
                    ) {
                        pageProgress ?: BookReaderProgress.Page(1, 1)
                    } else {
                        BookReaderProgress.Chapter(
                            value = visualChapterProgression,
                            style = readingProgressStyleSetting.effectiveValue,
                            activeColor = readerPalette.progress,
                            trackColor = readerPalette.fill,
                        )
                    }
                } else {
                    null
                },
                footerColor = readerPalette.background,
                // The status bar shows the time and battery already.
                showTimeAndBattery = showTimeAndBatterySetting.effectiveValue && !showStatusBarSetting.effectiveValue,
                translationController = selectionCoordinator?.translationController,
                translationSpeechState = translationSpeechState,
                onTranslationSpeechToggle = selectionCoordinator?.let { it::toggleTranslationSpeech },
                onTranslationPopupBoundsChanged = onTranslationPopupBoundsChanged,
                translationSnackbarHostState = translationSnackbarHostState,
                onRootPositionInWindow = { rootPosition = it },
                modifier = Modifier
                    .fillMaxSize()
                    .background(readerPalette.background),
                content = {
                    BookDocumentReaderViewport(
                        settings = settingBindings,
                        onPageProgress = { pageProgress = it },
                        onSeekPages = seekState::updatePages,
                        onViewportLocation = {
                            seekState.observe(it)
                            onViewportLocation(it)
                        },
                        chromeVisible = state.chromeVisible,
                        currentChapter = state.window.current,
                        currentChapterId = state.currentChapterId,
                        window = state.window,
                        loadedSections = state.loadedSections,
                        loadStates = state.loadStates,
                        navigationRequest = state.navigationRequest,
                        textSizePercent = textSizeSetting.effectiveValue,
                        onLocation = onLocation,
                        onChapterBoundaryReached = onChapterBoundaryReached,
                        onTransitionRetry = onTransitionRetry,
                        onChapterEndObservation = onChapterEndObservation,
                        onAnchorMissing = onAnchorMissing,
                        onInternalLinkClick = onInternalLinkClick,
                        onExternalLinkClick = onExternalLinkClick,
                        onScrollStarted = onChromeHide,
                        onUserScrollStarted = {
                            seekState.cancelSeek()
                            onUserScrollStarted()
                        },
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
                    state.chapterSelection?.let { selection ->
                        BookDocumentChapterSelectionFeedback(
                            selection = selection,
                            onRetry = onChapterSelectionRetry,
                            onDismiss = onChapterSelectionDismiss,
                        )
                    }
                },
                overlay = { progressIndicator ->
                    ReaderChrome(
                        // Searching keeps the bar with the query up while the reader scrolls to each match.
                        visible = state.chromeVisible || searching,
                        retainBars = true,
                        modifier = Modifier.fillMaxSize(),
                        persistentBottomContent = {
                            Box(
                                Modifier.graphicsLayer { alpha = if (state.chromeVisible) 0f else 1f }
                                    .then(if (state.chromeVisible) Modifier.clearAndSetSemantics { } else Modifier),
                            ) {
                                progressIndicator()
                            }
                        },
                        topBar = {
                            BookDocumentReaderTopBar(
                                state = state,
                                search = search,
                                onOpenSearch = { search.open(jumpHistory.location) },
                                onStepSearch = { forward -> search.step(forward)?.let(onRevealSearchMatch) },
                                onSearchAllChapters = { bookSearchVisible = true },
                                onCloseSearch = closeSearch,
                                onToggleBookmark = onToggleBookmark,
                                onChildWebViewAction = onChildWebViewAction,
                                onClose = onClose,
                            )
                        },
                        bottomBar = {
                            if (!searching) {
                                Column {
                                    BookDocumentSeekControls(
                                        seekState,
                                        state,
                                        readingModeSetting.effectiveValue,
                                        jumpHistory,
                                        onNavigate = currentOnNavigationSelected,
                                        onReturn = onReturn,
                                    )
                                    ReaderChromeBottomBar {
                                        BookDocumentReadingModeShortcut(settingBindings.readingMode)
                                        ReaderChromeBottomBarAction(
                                            icon = MaterialSymbols.AutoMirroredRounded.ViewList,
                                            contentDescription = stringResource(MR.strings.reader_table_of_contents),
                                            onClick = { onNavigationVisibilityChange(true) },
                                        )
                                        ReaderChromeBottomBarAction(
                                            icon = MaterialSymbols.Rounded.Settings,
                                            contentDescription = stringResource(MR.strings.action_settings),
                                            onClick = { onSettingsVisibilityChange(true) },
                                        )
                                    }
                                }
                            }
                        },
                    )
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = KatariTheme.sizes.touchTarget + KatariTheme.spacing.s)
                            .windowInsetsPadding(
                                WindowInsets.safeDrawing.only(
                                    WindowInsetsSides.Bottom + WindowInsetsSides.Horizontal,
                                ),
                            ),
                    )
                },
            )

            BookDocumentChapterSearchMatching(
                search = search,
                chapterId = state.currentChapterId,
                sections = state.loadedSections[state.currentChapterId]?.sections,
                onReveal = onRevealSearchMatch,
            )
            BackHandler(enabled = searching, onBack = closeSearch)
            if (bookSearchVisible) {
                BookDocumentBookSearchSheet(
                    query = search.query.orEmpty(),
                    bookSearch = bookSearch,
                    onResultClick = { result ->
                        bookSearchVisible = false
                        search.showResult(result)?.let(onRevealSearchMatch)
                            ?: currentOnNavigationSelected(result.target)
                    },
                    onDismissRequest = { bookSearchVisible = false },
                )
            }

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
                EntryReaderNavigationSheet(
                    visible = true,
                    rows = navigation.rows,
                    selectedIndex = navigation.selectedIndex,
                    onItemClick = { pendingNavigation = it },
                    onDismissRequest = { onNavigationVisibilityChange(false) },
                )
            }
            if (state.settingsVisible) {
                ReaderSettingsDialogHost(
                    sharedSettingsRegistry = sharedSettingsRegistry,
                    settingsSurfaceId = BookDocumentReaderProcessor.SETTINGS_SURFACE_ID,
                    capabilities = BookDocumentReaderProcessor.CAPABILITIES,
                    sharedSettingBindings = settingBindings.sharedSettings,
                    sharedTabTitle = stringResource(MR.strings.reader_shared_settings),
                    onDismissRequest = { onSettingsVisibilityChange(false) },
                    onOpenDefaultSettings = onOpenDefaultSettings,
                    sharedSettingDetails = selectionCoordinator?.translationController?.let { controller ->
                        mapOf(
                            BookAutomaticTranslationSettingsProvider.AUTOMATIC_SELECTION_SETTING_ID to
                                bookTranslationLanguageDetails(controller.hostCoordinator, controller.declaredLanguage),
                        )
                    }.orEmpty(),
                    snackbarHostState = settingsSnackbarHostState,
                    onResetProcessorSettings = settingBindings::clearProcessorEntryOverrides,
                    processorTabTitles = listOf(
                        androidStringResource(R.string.book_reader_appearance_settings),
                        androidStringResource(R.string.book_reader_behavior_settings),
                    ),
                    content = { page ->
                        when (page) {
                            0 -> BookDocumentReaderAppearancePage(settingBindings, fonts)
                            1 -> BookDocumentReaderBehaviorPage(
                                bindings = settingBindings,
                                progressStyleAvailable =
                                readingModeSetting.effectiveValue == BookDocumentReadingMode.SCROLL,
                            )
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
