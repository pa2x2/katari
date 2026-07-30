package mihon.entry.interactions.book.prose

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import mihon.entry.interactions.EntryChildWebViewAction
import mihon.entry.interactions.EntryChildWebViewActionsMenu
import mihon.entry.interactions.EntryChildWebViewResolution
import mihon.entry.interactions.book.BookReaderLayoutButton
import mihon.entry.interactions.book.BookReaderLoadingScreen
import mihon.entry.interactions.book.BookReaderNavigationRow
import mihon.entry.interactions.book.BookReaderNavigationSheet
import mihon.entry.interactions.book.BookReaderProgress
import mihon.entry.interactions.book.BookReaderScaffold
import mihon.entry.interactions.book.BookReaderTextSelection
import mihon.entry.interactions.book.BookSelectionTranslationController
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.model.BookDocumentPosition
import mihon.entry.interactions.book.document.reader.BookDocumentTextInteraction
import mihon.entry.interactions.book.document.reader.BookDocumentTextSelection
import mihon.entry.interactions.book.document.reader.LocalBookDocumentTextInteraction
import mihon.entry.interactions.reader.settings.BookReaderLayoutMode
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.interactions.viewer.EntryChildWindow
import mihon.translation.ui.session.TranslationSelectionAnchor
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.MR
import tachiyomi.presentation.core.components.reader.ReaderChrome
import tachiyomi.presentation.core.components.reader.ReaderPageNavigator
import tachiyomi.presentation.core.components.reader.ReaderPageNavigatorType
import tachiyomi.presentation.core.components.reader.ReaderProgressNavigator
import tachiyomi.presentation.core.i18n.stringResource as i18nStringResource

internal data class HtmlProseReaderUiState(
    val entryTitle: String,
    val currentChapterId: Long,
    val chapters: List<EntryChapter>,
    val window: EntryChildWindow<EntryChapter>,
    val loadedChapters: Map<Long, HtmlProseLoadedChapter>,
    val viewerResetKey: Long = 0,
    val menuVisible: Boolean = false,
    val chapterListVisible: Boolean = false,
    val settingsVisible: Boolean = false,
    val childWebView: EntryChildWebViewResolution.Available? = null,
    val loadingChapterId: Long? = null,
    val loadError: String? = null,
    val transitionLoadStates: Map<Long, HtmlProseChapterLoadState> = emptyMap(),
)

internal sealed interface HtmlProseChapterLoadState {
    data object Loading : HtmlProseChapterLoadState

    data class Failed(val message: String) : HtmlProseChapterLoadState
}

@Composable
internal fun HtmlProseReaderScreen(
    state: HtmlProseReaderUiState,
    settings: HtmlProseSettingsBinding,
    onLocation: (chapterId: Long, progression: Float, position: BookDocumentPosition?) -> Unit,
    onChapterEntered: (EntryChapter) -> Unit,
    onClose: () -> Unit,
    onMenuVisibilityChange: (Boolean) -> Unit,
    onChapterListVisibilityChange: (Boolean) -> Unit,
    onChapterSelected: (EntryChapter) -> Unit,
    onTransitionChapterRequested: (EntryChapter) -> Unit,
    onTransitionChapterRetry: (EntryChapter) -> Unit,
    onSettingsVisibilityChange: (Boolean) -> Unit,
    onChildWebViewAction: (EntryChildWebViewAction, EntryChildWebViewResolution.Available) -> Unit,
    onExternalLinkClick: (String) -> Unit,
    translationController: BookSelectionTranslationController? = null,
) {
    val theme by settings.theme.state.collectEffectiveValue()
    val fontFamily by settings.fontFamily.state.collectEffectiveValue()
    val fontSize by settings.fontSize.state.collectEffectiveValue()
    val lineHeight by settings.lineHeight.state.collectEffectiveValue()
    val pageMargins by settings.pageMargins.state.collectEffectiveValue()
    val textAlignment by settings.textAlignment.state.collectEffectiveValue()
    val layoutMode by settings.layoutMode.state.collectEffectiveValue()
    val tapNavigation by settings.tapNavigation.state.collectEffectiveValue()
    val showProgress by settings.showProgress.state.collectEffectiveValue()
    val drawUnderCutout by settings.drawUnderCutout.state.collectEffectiveValue()
    val readerLayoutMode = BookReaderLayoutMode.fromSerializedValue(layoutMode)
    val paginated = readerLayoutMode == BookReaderLayoutMode.PAGINATED
    val palette = prosePalette(theme, isSystemInDarkTheme())
    var position by remember(state.currentChapterId) {
        val loaded = state.loadedChapters[state.currentChapterId]
        val progression = loaded?.document?.document?.progressionAt(loaded.initialPosition) ?: 0f
        mutableStateOf(
            ProseViewerPosition(
                chapterId = state.currentChapterId,
                progression = progression,
                currentPage = 1,
                totalPages = 1,
                documentPosition = loaded?.initialPosition,
            ),
        )
    }
    var viewerActions by remember { mutableStateOf(ProseViewerActions()) }
    var readerRootPositionInWindow by remember { mutableStateOf(Offset.Zero) }
    val automaticTranslationEnabled =
        translationController?.effectiveEnabled?.collectAsState()?.value == true
    val preparationToken = remember(
        state.viewerResetKey,
        paginated,
        fontFamily,
        fontSize,
        lineHeight,
        pageMargins,
        textAlignment,
        drawUnderCutout,
    ) {
        Any()
    }
    var preparedToken by remember { mutableStateOf<Any?>(null) }
    val viewerPrepared = preparedToken === preparationToken
    val onViewerPrepared = { preparedToken = preparationToken }

    CompositionLocalProvider(
        LocalBookDocumentTextInteraction provides BookDocumentTextInteraction(
            observeSelections = automaticTranslationEnabled,
            rootPositionInWindow = readerRootPositionInWindow,
            onSelection = { selection ->
                when (selection) {
                    is BookDocumentTextSelection.Changed -> translationController?.submitSelection(
                        BookReaderTextSelection(
                            ownerIdentity = selection.ownerIdentity,
                            identity = selection.identity,
                            text = selection.text,
                            anchor = selection.boundsInReaderRoot.let { bounds ->
                                TranslationSelectionAnchor(
                                    left = bounds.left,
                                    top = bounds.top,
                                    right = bounds.right,
                                    bottom = bounds.bottom,
                                )
                            },
                        ),
                    )
                    is BookDocumentTextSelection.Cleared ->
                        translationController?.clearSelection(selection.ownerIdentity)
                }
            },
            isReaderTapBlocked = {
                translationController?.isTranslationActive() == true
            },
            onBlockedReaderTap = {
                translationController?.dismissTranslation()
            },
            onNonLinkTap = { x, width ->
                if (translationController?.dismissTranslationOnReaderTap() != true) {
                    viewerActions.onTapFraction(x / width.coerceAtLeast(1f))
                }
            },
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = palette.background,
        ) {
            BookReaderScaffold(
                progress = if (!showProgress) {
                    null
                } else if (paginated) {
                    BookReaderProgress.Page(position.currentPage, position.totalPages)
                } else {
                    BookReaderProgress.Percentage((position.progression * 100).toInt())
                },
                progressVisible = !state.menuVisible,
                footerColor = palette.background,
                modifier = Modifier.fillMaxSize(),
                translationController = translationController,
                onRootPositionInWindow = { readerRootPositionInWindow = it },
                content = {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .then(
                                if (drawUnderCutout) {
                                    Modifier
                                } else {
                                    Modifier.windowInsetsPadding(WindowInsets.displayCutout)
                                },
                            ),
                    ) {
                        key(state.viewerResetKey) {
                            if (paginated) {
                                PaginatedProseViewer(
                                    state = state,
                                    initialProgression = position.progression,
                                    initialDocumentPosition = position.documentPosition,
                                    palette = palette,
                                    fontFamily = fontFamily,
                                    fontSizePercent = fontSize,
                                    lineHeightPercent = lineHeight,
                                    pageMarginsPercent = pageMargins,
                                    textAlignment = textAlignment,
                                    tapNavigation = tapNavigation,
                                    chromeVisible = state.menuVisible,
                                    preparationToken = preparationToken,
                                    onPosition = {
                                        position = it
                                        onLocation(it.chapterId, it.progression, it.documentPosition)
                                    },
                                    onChapterEntered = onChapterEntered,
                                    onTransitionChapterRequested = onTransitionChapterRequested,
                                    onTransitionChapterRetry = onTransitionChapterRetry,
                                    onMenuToggle = { onMenuVisibilityChange(!state.menuVisible) },
                                    onExternalLinkClick = onExternalLinkClick,
                                    onActions = { viewerActions = it },
                                    onPrepared = onViewerPrepared,
                                )
                            } else {
                                ScrollingProseViewer(
                                    state = state,
                                    initialProgression = position.progression,
                                    initialDocumentPosition = position.documentPosition,
                                    palette = palette,
                                    fontFamily = fontFamily,
                                    fontSizePercent = fontSize,
                                    lineHeightPercent = lineHeight,
                                    pageMarginsPercent = pageMargins,
                                    textAlignment = textAlignment,
                                    preparationToken = preparationToken,
                                    onPosition = {
                                        position = it
                                        onLocation(it.chapterId, it.progression, it.documentPosition)
                                    },
                                    onChapterEntered = onChapterEntered,
                                    onTransitionChapterRequested = onTransitionChapterRequested,
                                    onTransitionChapterRetry = onTransitionChapterRetry,
                                    onMenuToggle = { onMenuVisibilityChange(!state.menuVisible) },
                                    onExternalLinkClick = onExternalLinkClick,
                                    onActions = { viewerActions = it },
                                    onPrepared = onViewerPrepared,
                                )
                            }
                        }
                    }
                },
                overlay = {
                    val chromeColor = MaterialTheme.colorScheme
                        .surfaceColorAtElevation(3.dp)
                        .copy(alpha = if (isSystemInDarkTheme()) 0.9f else 0.95f)
                    ReaderChrome(
                        visible = state.menuVisible,
                        topBar = {
                            TopAppBar(
                                modifier = Modifier.background(chromeColor),
                                title = {
                                    Column {
                                        Text(state.entryTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text(state.window.current.name, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    }
                                },
                                navigationIcon = {
                                    IconButton(onClick = onClose) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.ArrowBack,
                                            stringResource(R.string.book_reader_close),
                                        )
                                    }
                                },
                                actions = {
                                    EntryChildWebViewActionsMenu(
                                        resolution = state.childWebView,
                                        onAction = onChildWebViewAction,
                                    )
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    scrolledContainerColor = Color.Transparent,
                                ),
                            )
                        },
                        bottomBar = {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (paginated) {
                                    ReaderPageNavigator(
                                        type = ReaderPageNavigatorType.HORIZONTAL_LTR,
                                        onNextSection = viewerActions.nextSection,
                                        nextSectionEnabled = state.window.next != null,
                                        onPreviousSection = viewerActions.previousSection,
                                        previousSectionEnabled = state.window.previous != null,
                                        currentPage = position.currentPage,
                                        totalPages = position.totalPages,
                                        onPageIndexChange = viewerActions.seekPage,
                                        showSinglePageLabel = true,
                                        previousSectionDescription = stringResource(
                                            R.string.prose_reader_previous_chapter,
                                        ),
                                        nextSectionDescription = stringResource(R.string.prose_reader_next_chapter),
                                    )
                                } else {
                                    ReaderProgressNavigator(
                                        isRtl = false,
                                        onNextSection = viewerActions.nextSection,
                                        nextSectionEnabled = state.window.next != null,
                                        onPreviousSection = viewerActions.previousSection,
                                        previousSectionEnabled = state.window.previous != null,
                                        currentProgress = position.progression,
                                        onProgressChange = viewerActions.seekProgress,
                                        onProgressChangeFinished = viewerActions.seekProgress,
                                        previousSectionDescription = stringResource(
                                            R.string.prose_reader_previous_chapter,
                                        ),
                                        nextSectionDescription = stringResource(R.string.prose_reader_next_chapter),
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(chromeColor)
                                        .navigationBarsPadding()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                ) {
                                    val scope = rememberCoroutineScope()
                                    BookReaderLayoutButton(
                                        layoutMode = readerLayoutMode,
                                        onClick = {
                                            val target = if (readerLayoutMode == BookReaderLayoutMode.PAGINATED) {
                                                BookReaderLayoutMode.SCROLLING
                                            } else {
                                                BookReaderLayoutMode.PAGINATED
                                            }
                                            scope.launch {
                                                settings.layoutMode.setEntryOverride(target.serializedValue)
                                            }
                                        },
                                    )
                                    IconButton(onClick = { onSettingsVisibilityChange(true) }) {
                                        Icon(Icons.Outlined.Settings, stringResource(R.string.prose_reader_settings))
                                    }
                                    IconButton(onClick = { onChapterListVisibilityChange(true) }) {
                                        Icon(
                                            Icons.AutoMirrored.Outlined.ViewList,
                                            i18nStringResource(MR.strings.book_table_of_contents),
                                        )
                                    }
                                }
                            }
                        },
                    )

                    state.loadError?.let { error ->
                        Surface(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            shape = MaterialTheme.shapes.large,
                            tonalElevation = 6.dp,
                        ) {
                            Text(error, modifier = Modifier.padding(24.dp), color = MaterialTheme.colorScheme.error)
                        }
                    }

                    if (!viewerPrepared) {
                        BookReaderLoadingScreen(
                            contentDescription = stringResource(R.string.book_reader_loading),
                        )
                    }
                },
            )
        }
    }

    BookReaderNavigationSheet(
        visible = state.chapterListVisible,
        rows = remember(state.chapters) {
            state.chapters.map { BookReaderNavigationRow(it, it.name) }
        },
        selectedIndex = state.chapters.indexOfFirst { it.id == state.currentChapterId },
        onItemClick = onChapterSelected,
        onDismissRequest = { onChapterListVisibilityChange(false) },
    )
    if (state.settingsVisible) {
        HtmlProseSettingsDialog(settings) { onSettingsVisibilityChange(false) }
    }
    BackHandler(enabled = state.chapterListVisible || state.settingsVisible) {
        if (state.chapterListVisible) onChapterListVisibilityChange(false) else onSettingsVisibilityChange(false)
    }
}
