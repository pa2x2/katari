package mihon.entry.interactions.book.document.reader

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalFocusManager
import androidx.navigationevent.NavigationEventInfo
import androidx.navigationevent.compose.NavigationBackHandler
import androidx.navigationevent.compose.rememberNavigationEventState
import mihon.entry.interactions.book.document.reader.picture.LocalBookDocumentPictureExport
import mihon.entry.interactions.book.document.reader.rules.BookDocumentHideTextDialog
import mihon.entry.interactions.book.reader.BookReaderDependencies
import mihon.entry.interactions.book.reader.BookReaderLoadingScreen
import mihon.entry.interactions.book.reader.failure.BookReaderChapterHeading
import mihon.entry.interactions.book.reader.failure.BookReaderFailureScreen
import mihon.entry.interactions.reader.settings.BookTextRuleScope
import mihon.entry.interactions.source.EntryChildWebViewAction
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The book reader [controller] runs, filling the window: loading, the failure that kept the book from opening, or the
 * book with the dialogs it raises. Back closes what is open over the book before it leaves the reader.
 */
@Composable
internal fun BookDocumentReaderContent(
    controller: BookDocumentReaderController,
    host: BookDocumentReaderHost,
    dependencies: BookReaderDependencies,
) {
    // Composed before the reader, so the search and failures inside it, which register later, take back first.
    NavigationBackHandler(
        state = rememberNavigationEventState(NavigationEventInfo.None),
        isBackEnabled = controller.handlesBack,
        onBackCompleted = { controller.handleBack() },
    )
    when (val surface = controller.surfaceState) {
        BookDocumentReaderSurfaceState.Loading -> BookReaderLoadingScreen(
            contentDescription = stringResource(MR.strings.book_reader_loading),
        )
        is BookDocumentReaderSurfaceState.Error -> BookDocumentReaderFailure(controller, host, surface)
        BookDocumentReaderSurfaceState.Ready -> {
            val state = controller.readerState
            val settingBindings = controller.settingBindings
            val fonts = controller.readerFonts
            if (state != null && settingBindings != null && fonts != null) {
                val focusManager = LocalFocusManager.current
                CompositionLocalProvider(
                    LocalBookDocumentResourceDecoding provides dependencies.resourceDecoding,
                    LocalBookDocumentPictureExport provides dependencies.imageExport,
                ) {
                    val coordinator = controller.chapterCoordinator
                    BookDocumentReaderScreen(
                        state = state,
                        visualChapterProgression = controller.visualChapterProgression,
                        settingBindings = settingBindings,
                        fonts = fonts,
                        bookSearch = controller.bookSearch,
                        selectionCoordinator = controller.selectionCoordinator,
                        keys = controller.keys,
                        host = host,
                        dependencies = dependencies,
                        onLocation = coordinator::onLocation,
                        onViewportLocation = controller.jumpHistory::observe,
                        jumpHistory = controller.jumpHistory,
                        onReturn = controller::returnToJumpOrigin,
                        onChapterBoundaryReached = { coordinator.loadChapter(it, activate = false, retry = true) },
                        onTransitionRetry = { coordinator.loadChapter(it, activate = false, retry = true) },
                        onChapterEndObservation = coordinator::onChapterEndObservation,
                        onNavigationSelected = { target ->
                            focusManager.clearFocus(force = true)
                            controller.selectNavigationTarget(target)
                        },
                        onRevealSearchMatch = { coordinator.revealSearchMatch(it.sectionKey, it.match.start) },
                        onSearchClosed = { returnTo ->
                            returnTo?.let { coordinator.selectNavigationTarget(it, rememberOrigin = false) }
                        },
                        onChromeToggle = controller::toggleChrome,
                        onChromeHide = { controller.setChromeVisible(false) },
                        onUserScrollStarted = coordinator::onUserScrollStarted,
                        onNavigationVisibilityChange = controller::setNavigationVisible,
                        onSettingsVisibilityChange = controller::setSettingsVisible,
                        onChildWebViewAction = controller::launchChildWebViewAction,
                        onToggleBookmark = controller::toggleCurrentChapterBookmark,
                        onHideText = controller::requestHideText,
                        snackbarHostState = controller.snackbarHostState,
                        onAnchorMissing = { controller.showReaderFeedback(MR.strings.book_document_anchor_missing) },
                        onInternalLinkClick = coordinator::navigateLink,
                        onExternalLinkClick = controller::confirmExternalLink,
                        onAuxiliaryDismiss = coordinator::dismissAuxiliarySection,
                        onChapterSelectionRetry = coordinator::retryChapterSelection,
                        onChapterSelectionDismiss = coordinator::dismissChapterSelection,
                    )
                }
            }
        }
    }
    BookDocumentReaderDialogs(controller)
}

@Composable
private fun BookDocumentReaderFailure(
    controller: BookDocumentReaderController,
    host: BookDocumentReaderHost,
    surface: BookDocumentReaderSurfaceState.Error,
) {
    val sourceChapter = surface.sourceChapter
    val webView = remember(sourceChapter) { sourceChapter?.let(controller::resolveFailureWebView) }
    val heading by produceState<BookReaderChapterHeading?>(null) { value = controller.loadFailureHeading() }
    BookReaderFailureScreen(
        reason = surface.reason,
        message = surface.message,
        heading = heading,
        onClose = host.onClose,
        onRetry = controller::retryOpen.takeIf { surface.canRetry },
        sourceName = sourceChapter?.sourceName,
        onOpenWebView = webView?.let {
            { controller.launchChildWebViewAction(EntryChildWebViewAction.OPEN_IN_WEB_VIEW, it) }
        },
    )
}

@Composable
private fun BookDocumentReaderDialogs(controller: BookDocumentReaderController) {
    controller.pendingHiddenText?.let { text ->
        val session = controller.currentSession()
        if (session == null) {
            SideEffect(controller::dismissHideText)
        } else {
            val owner = session.owner
            BookDocumentHideTextDialog(
                selectedText = text,
                series = BookTextRuleScope.Series(owner.source, owner.url, owner.displayTitle),
                source = BookTextRuleScope.Source(owner.source, session.sourceName ?: owner.source.toString()),
                onSave = controller::addTextRules,
                onDismiss = controller::dismissHideText,
            )
        }
    }
    controller.pendingExternalLink?.let { url ->
        BookDocumentExternalLinkDialog(
            host = externalLinkHost(url).orEmpty(),
            onOpen = controller::openPendingExternalLink,
            onDismiss = controller::dismissExternalLink,
        )
    }
    controller.pendingRemoteResourceConsent?.let { pending ->
        BookRemoteResourceConsentDialog(
            pending = pending,
            onAllow = { controller.resolveRemoteResourceConsent(allow = true) },
            onBlock = { controller.resolveRemoteResourceConsent(allow = false) },
        )
    }
}
