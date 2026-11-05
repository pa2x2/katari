package eu.kanade.tachiyomi.ui.reader.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.zIndex
import eu.kanade.presentation.reader.DisplayRefreshHost
import eu.kanade.presentation.reader.OrientationSelectDialog
import eu.kanade.presentation.reader.ReaderChapterNavigationSheet
import eu.kanade.presentation.reader.ReaderContentOverlay
import eu.kanade.presentation.reader.ReaderPageActionsDialog
import eu.kanade.presentation.reader.ReaderPageActionsMenu
import eu.kanade.presentation.reader.ReadingModeSelectDialog
import eu.kanade.presentation.reader.appbars.ReaderAppBars
import eu.kanade.presentation.reader.appbars.ReaderMenuBounds
import eu.kanade.presentation.reader.components.ChapterNavigatorType
import eu.kanade.presentation.reader.settings.ReaderSettingsDialog
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.setting.ReaderSettingsScreenModel
import eu.kanade.tachiyomi.ui.reader.startup.ReaderStartupErrorScreen
import eu.kanade.tachiyomi.ui.reader.startup.ReaderStartupLoadingScreen
import eu.kanade.tachiyomi.ui.reader.startup.ReaderStartupState
import mihon.entry.interactions.manga.reader.settings.MangaReaderSettingsBindings
import mihon.entry.interactions.reader.settings.MangaReaderSettings
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.viewer.chrome.ui.position.ReaderPageIndicator
import mihon.entry.viewer.chrome.ui.position.ReaderUnderPages
import mihon.presentation.kit.dialog.ProgressDialog
import mihon.presentation.kit.feedback.ScreenMessagesOverlay
import mihon.presentation.kit.feedback.rememberScreenMessages
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * Everything the reader draws over the pages: the startup screens, the page number, the colour and brightness
 * overlays, the controls, the host's text features, the reader's sheets and dialogs and its messages. The viewer
 * showing the pages is under it, and [controller] drives it.
 */
@Composable
internal fun MangaReaderScreen(controller: MangaReaderController, host: MangaReaderScreenHost) {
    val viewModel = controller.viewModel
    val state by viewModel.state.collectAsState()
    val messages = rememberScreenMessages()
    val menuBounds = remember { ReaderMenuBounds() }
    // Before the notices are collected, so it holds the screen's messages by the time the first one comes
    ReaderMessages(state.menuVisible, menuBounds)
    LaunchedEffect(controller) {
        controller.notices.collect { messages.show(it) }
    }
    when (val startup = state.startupState) {
        ReaderStartupState.Loading -> {
            ReaderStartupLoadingScreen()
            return
        }
        is ReaderStartupState.Failed -> {
            ReaderStartupErrorScreen(
                failure = startup,
                onRetry = viewModel::retryInitialChapter,
                onClose = host.onClose,
            )
            return
        }
        ReaderStartupState.Ready -> Unit
    }
    val settings = viewModel.settingsBindings.collectAsState().value ?: return
    val effectiveReadingMode by viewModel.effectiveReadingMode.collectAsState()
    val settingsScreenModel = remember(settings) {
        ReaderSettingsScreenModel(
            effectiveReadingMode = viewModel.effectiveReadingMode,
            settings = settings,
            onChangeReadingMode = viewModel::setMangaReadingMode,
            onChangeOrientation = viewModel::setMangaOrientationType,
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        PageNumber(controller, settings, state, rightToLeft = effectiveReadingMode == ReadingMode.RIGHT_TO_LEFT)
        ContentOverlay(controller, settings, state)
        AppBars(controller, host, settings, state, effectiveReadingMode, menuBounds)
        host.textLayer?.invoke(state.menuVisible, menuBounds)
    }

    val onDismissRequest = viewModel::closeDialog
    when (val dialog = state.dialog) {
        is ReaderViewModel.Dialog.Loading -> ProgressDialog(message = stringResource(MR.strings.loading))
        is ReaderViewModel.Dialog.Settings -> ReaderSettingsDialog(
            onDismissRequest = onDismissRequest,
            onOpenDefaultSettings = host.onOpenDefaultSettings,
            onShowMenus = { controller.setMenuVisibility(true) },
            onHideMenus = { controller.setMenuVisibility(false) },
            screenModel = settingsScreenModel,
            sharedSettingsRegistry = host.sharedSettingsRegistry,
            hasDisplayCutout = host.hasDisplayCutout(),
        )
        is ReaderViewModel.Dialog.ReadingModeSelect -> ReadingModeSelectDialog(
            onDismissRequest = onDismissRequest,
            screenModel = settingsScreenModel,
            onChange = { mode ->
                // A new viewer names its reading mode itself when that setting is on
                if (!settings.showReadingMode.state.value.effectiveValue) messages.show(mode)
            },
        )
        is ReaderViewModel.Dialog.OrientationModeSelect -> OrientationSelectDialog(
            onDismissRequest = onDismissRequest,
            screenModel = settingsScreenModel,
            onChange = { orientation -> messages.show(orientation) },
        )
        is ReaderViewModel.Dialog.ChapterNavigation -> ReaderChapterNavigationSheet(
            presentation = dialog.presentation,
            currentChapterId = state.currentChapter?.chapter?.id,
            onChapterClick = controller::loadChapter,
            onDismissRequest = onDismissRequest,
        )
        is ReaderViewModel.Dialog.PageActions -> {
            val onCopy = { viewModel.shareImage(host.context, copyToClipboard = true) }
                .takeIf { viewModel.offersPageCopying }
            val onShare = { viewModel.shareImage(host.context, copyToClipboard = false) }
                .takeIf { viewModel.offersPageSharing }
            val onSave = { viewModel.saveImage(host.context) }.takeIf { viewModel.offersPageSaving }
            val menuAt = dialog.menuAt
            if (menuAt != null) {
                ReaderPageActionsMenu(menuAt, onDismissRequest, viewModel::setAsCover, onCopy, onShare, onSave)
            } else {
                ReaderPageActionsDialog(onDismissRequest, viewModel::setAsCover, onCopy, onShare, onSave)
            }
        }
        null -> Unit
    }
}

/**
 * The screen's messages over everything the reader draws, clear of the bottom menu while it shows. Where the screen
 * has no messages of its own, as in Android's reader activity, they are toasts and this draws nothing.
 */
@Composable
private fun ReaderMessages(menuVisible: Boolean, menuBounds: ReaderMenuBounds) {
    var bottom by remember { mutableFloatStateOf(0f) }
    val menuTop = menuBounds.bottomMenuTop?.takeIf { menuVisible }
    val clearance = with(LocalDensity.current) { (menuTop?.let { bottom - it } ?: 0f).coerceAtLeast(0f).toDp() }
    ScreenMessagesOverlay(
        modifier = Modifier
            .zIndex(1f)
            .onGloballyPositioned { bottom = it.positionInRoot().y + it.size.height }
            .padding(bottom = clearance),
    )
}

@Composable
private fun PageNumber(
    controller: MangaReaderController,
    settings: MangaReaderSettingsBindings,
    state: ReaderViewModel.State,
    rightToLeft: Boolean,
) {
    val showPageNumber by settings.showPageNumber.state.collectAsState()
    if (state.menuVisible || !showPageNumber.effectiveValue) return
    Box(modifier = Modifier.fillMaxSize()) {
        val facingPage = state.facingPage
        ReaderUnderPages(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .navigationBarsPadding(),
            facing = if (facingPage != null) {
                { ReaderPageIndicator(facingPage, state.totalPages, Modifier.align(Alignment.Center)) }
            } else {
                null
            },
            rightToLeft = rightToLeft,
            pageMiddles = controller.spreadPageMiddles?.takeIf { it.first == state.currentPage }?.second,
        ) {
            ReaderPageIndicator(state.currentPage, state.totalPages, Modifier.align(Alignment.Center))
        }
    }
}

@Composable
private fun ContentOverlay(
    controller: MangaReaderController,
    settings: MangaReaderSettingsBindings,
    state: ReaderViewModel.State,
) {
    val flashOnPageChange by settings.flashOnPageChange.state.collectAsState()
    val colorOverlayEnabled by settings.colorFilter.state.collectAsState()
    val colorOverlay by settings.colorFilterValue.state.collectAsState()
    val colorOverlayMode by settings.colorFilterMode.state.collectAsState()
    val colorOverlayBlendMode = remember(colorOverlayMode.effectiveValue) {
        MangaReaderSettings.ColorFilterMode.getOrNull(colorOverlayMode.effectiveValue)?.second
    }

    ReaderContentOverlay(
        brightness = state.brightnessOverlayValue,
        color = colorOverlay.effectiveValue.takeIf { colorOverlayEnabled.effectiveValue },
        colorBlendMode = colorOverlayBlendMode,
    )

    val refreshHost = controller.displayRefresh
    if (flashOnPageChange.effectiveValue && refreshHost != null) {
        DisplayRefreshHost(hostState = refreshHost)
    }
}

@Composable
private fun AppBars(
    controller: MangaReaderController,
    host: MangaReaderScreenHost,
    settings: MangaReaderSettingsBindings,
    state: ReaderViewModel.State,
    effectiveReadingMode: ReadingMode,
    menuBounds: ReaderMenuBounds,
) {
    val viewModel = controller.viewModel
    if (!viewModel.sourcesInitialized.collectAsState().value) return
    val viewer by controller.viewer.collectAsState()

    val cropBorderPaged by settings.cropBorders.state.collectAsState()
    val cropBorderWebtoon by settings.cropBordersWebtoon.state.collectAsState()
    val autoScrollFeatureEnabled by settings.autoScrollEnabled.state.collectAsState()
    val isPagerType = effectiveReadingMode.type == ReadingMode.ViewerType.Pager
    val cropEnabled = if (isPagerType) cropBorderPaged.effectiveValue else cropBorderWebtoon.effectiveValue
    val showAutoScrollToggle = autoScrollFeatureEnabled.effectiveValue && viewer?.supportsAutoScroll() == true

    val textState by viewModel.textSession.state.collectAsState()
    val verticalNavigatorModes by settings.verticalNavigator.state.collectAsState()
    val verticalNavigator = verticalNavigatorModes.effectiveValue.contains(effectiveReadingMode)
    val verticalNavigatorOnLeft by settings.verticalNavigatorOnLeft.state.collectAsState()
    val verticalNavigatorHeight by settings.verticalNavigatorHeight.state.collectAsState()

    ReaderAppBars(
        visible = state.menuVisible,

        mangaTitle = state.manga?.displayTitle,
        chapterTitle = state.currentChapter?.chapter?.name,
        navigateUp = host.onNavigateUp,
        onClickTopAppBar = host.onOpenEntry,
        bookmarked = state.bookmarked,
        onToggleBookmarked = viewModel::toggleChapterBookmark,
        childWebView = controller.chapterWebView,
        onChildWebViewAction = host.onOpenWebView,
        webViewActions = host.webViewActions,
        onShiftPages = viewer?.takeIf { state.showsTwoPages }?.let { it::shiftPages },

        chapterNavigatorType = when {
            !verticalNavigator && effectiveReadingMode == ReadingMode.RIGHT_TO_LEFT ->
                ChapterNavigatorType.HORIZONTAL_RTL
            !verticalNavigator -> ChapterNavigatorType.HORIZONTAL_LTR
            verticalNavigatorOnLeft.effectiveValue -> ChapterNavigatorType.VERTICAL_LEFT
            else -> ChapterNavigatorType.VERTICAL_RIGHT
        },
        verticalNavigatorHeight = verticalNavigatorHeight.effectiveValue / 100f,
        onNextChapter = controller::loadNextChapter,
        enabledNext = state.viewerChapters?.next != null,
        onPreviousChapter = controller::loadPreviousChapter,
        enabledPrevious = state.viewerChapters?.previous != null,
        currentPage = state.currentPage,
        totalPages = state.totalPages,
        onPageIndexChange = controller::seekToPage,
        onPageIndexChangeFinished = controller::finishSeek,
        returnPositionAvailable = viewModel.jumpHistory.returnTarget != null,
        onReturnToPreviousPosition = controller::returnToPreviousPosition,
        onDismissReturnPosition = viewModel.jumpHistory::dismiss,

        readingMode = ReadingMode.fromPreference(viewModel.getMangaReadingMode(resolveDefault = false)),
        onClickReadingMode = viewModel::openReadingModeSelectDialog,
        orientation = ReaderOrientation.fromPreference(viewModel.getMangaOrientation(resolveDefault = false)),
        onClickOrientation = viewModel::openOrientationModeSelectDialog,
        cropEnabled = cropEnabled,
        onClickCropBorder = controller::toggleCropBorders,
        showAutoScrollToggle = showAutoScrollToggle,
        autoScrollActive = controller.isAutoScrollRunning,
        onClickAutoScroll = controller::toggleAutoScroll,
        onClickChapterNavigation = viewModel::openChapterNavigationDialog,
        textTranslationActive = textState.active,
        onClickTextTranslation = { controller.textInteraction.setActive(!textState.active) }
            .takeIf { host.textLayer != null },
        onClickSettings = viewModel::openSettingsDialog,
        menuBounds = menuBounds,
    )
}
