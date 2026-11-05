package mihon.entry.interactions.book.reader.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.zacsweers.metro.Inject
import mihon.core.platform.PlatformContext
import mihon.core.platform.currentPlatformContext
import mihon.core.platform.power.ScreenIdleInhibitor
import mihon.entry.interactions.book.document.reader.BookDocumentReaderContent
import mihon.entry.interactions.book.document.reader.BookDocumentReaderController
import mihon.entry.interactions.book.document.reader.BookDocumentReaderHost
import mihon.entry.interactions.book.document.reader.BookDocumentReaderProcessor
import mihon.entry.interactions.book.document.reader.BookDocumentReaderSessionViewModel
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderPreferences
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.interactions.book.reader.BookReaderDependencies
import mihon.entry.interactions.book.reader.BookReaderHostContent
import mihon.entry.interactions.book.reader.BookReaderHostController
import mihon.entry.interactions.reader.window.BookReader
import mihon.entry.interactions.reader.window.BookReaderNavigation
import mihon.entry.interactions.reader.window.BookReaderWindowRequest
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.viewer.chrome.ui.window.KeepScreenAwake
import mihon.entry.viewer.chrome.ui.window.ViewerFullScreen
import mihon.entry.viewer.chrome.ui.window.rememberReaderPointer
import mihon.presentation.kit.feedback.rememberScreenMessages
import mihon.presentation.kit.window.LocalWindowFullScreen
import mihon.presentation.kit.window.ReportScreenTitle
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The desktop's [BookReader]: the shared open flow, then the shared reader, as Android's two activities host them. It
 * takes the keyboard ([BookReaderKeys]), hides the mouse pointer while it rests over the book ([rememberReaderPointer]),
 * offers the window's full screen, keeps the screen awake as the reader's setting says, shows a guide to the mouse and
 * keys the first time it opens in a profile ([BookReaderInputGuide]), and names the book and chapter in the window's
 * title while it fills the window. There is no speech engine on the desktop yet, so Listen stays out.
 */
@Inject
internal class DesktopBookReader(
    private val dependencies: BookReaderDependencies,
    private val preferences: BookDocumentReaderPreferences,
    private val screenIdleInhibitor: ScreenIdleInhibitor,
) : BookReader {
    @Composable
    override fun Content(request: BookReaderWindowRequest, navigation: BookReaderNavigation) {
        val latestNavigation by rememberUpdatedState(navigation)
        val context = currentPlatformContext()
        // Kept by the screen showing the reader, which leaves composition while a screen opened over it shows. A
        // window's reader never outlives the app, so nothing is restored into it.
        val retainedSessions = viewModel { BookDocumentReaderSessionViewModel(SavedStateHandle()) }
        var opened by remember(request) { mutableStateOf<OpenedBook?>(null) }
        ViewerFullScreen(LocalWindowFullScreen.current) {
            val book = opened
            when {
                // Back from a screen opened over it, the reader takes up its retained sessions as Android's recreated
                // activity does, without opening the book again.
                book != null || retainedSessions.currentSession() != null -> Reader(
                    book ?: OpenedBook(BookReaderRequest(request.entryId, request.chapterId), sessionToken = null),
                    retainedSessions,
                    context,
                    latestNavigation,
                )
                else -> OpenFlow(request, context, latestNavigation) { opened = it }
            }
        }
    }

    @Composable
    private fun OpenFlow(
        request: BookReaderWindowRequest,
        context: PlatformContext,
        navigation: BookReaderNavigation,
        onOpened: (OpenedBook) -> Unit,
    ) {
        val scope = rememberCoroutineScope()
        val controller = remember(request) {
            BookReaderHostController(
                request = BookReaderRequest(request.entryId, request.chapterId),
                dependencies = dependencies,
                context = context,
                scope = scope,
                onOpened = { processor, openedRequest, sessionToken ->
                    check(processor.id == BookDocumentReaderProcessor.PROCESSOR_ID) {
                        "Book reader ${processor.id} has no desktop reader"
                    }
                    onOpened(OpenedBook(openedRequest, sessionToken))
                },
            )
        }
        LaunchedEffect(controller) { controller.start() }
        BookReaderHostContent(
            controller = controller,
            onClose = navigation::close,
            onOpenWebView = { sourceChapter, webView ->
                openWebView(context, navigation, webView, sourceChapter.owner.displayTitle)
            },
        )
    }

    @Composable
    private fun Reader(
        book: OpenedBook,
        retainedSessions: BookDocumentReaderSessionViewModel,
        context: PlatformContext,
        navigation: BookReaderNavigation,
    ) {
        var generation by remember { mutableIntStateOf(0) }
        key(generation) {
            val scope = rememberCoroutineScope()
            val messages = rememberScreenMessages()
            val reopening = remember { ReopenFlag() }
            val host = remember(context) {
                BookDocumentReaderHost(
                    context = context,
                    webViewActions = buildList {
                        if (navigation.webViewAvailable) add(EntryChildWebViewAction.OPEN_IN_WEB_VIEW)
                        add(EntryChildWebViewAction.OPEN_IN_BROWSER)
                    },
                    speaksSelections = false,
                    systemBars = false,
                    selectionToolbar = false,
                    showMessage = { message -> messages.show(message) },
                    onClose = navigation::close,
                    onChildWebViewAction = { action, resolution, title ->
                        when (action) {
                            EntryChildWebViewAction.OPEN_IN_WEB_VIEW ->
                                openWebView(context, navigation, resolution, title)
                            EntryChildWebViewAction.OPEN_IN_BROWSER ->
                                dependencies.linkOpener.open(context, resolution.url)
                            EntryChildWebViewAction.SHARE -> error("The desktop book reader offers no sharing")
                        }
                    },
                    onOpenDefaultSettings = navigation::openReaderSettings,
                    onReopen = {
                        reopening.value = true
                        generation++
                    },
                    // Only Android's floating selection toolbar has to keep clear of the popup.
                    onTranslationPopupBoundsChanged = {},
                )
            }
            val controller = remember(host) {
                BookDocumentReaderController(
                    request = book.request,
                    sessionToken = book.sessionToken,
                    retainedSessions = retainedSessions,
                    dependencies = dependencies,
                    host = host,
                    scope = scope,
                )
            }
            LaunchedEffect(controller) { controller.start() }
            DisposableEffect(controller) {
                onDispose(controller::close)
            }
            FollowLifecycle(controller, persistOnLeave = { !reopening.value })
            KeepScreenAwake(
                active = controller.keepsScreenAwake,
                reason = stringResource(MR.strings.reader_keeps_screen_awake),
                inhibitor = screenIdleInhibitor,
            )
            ReportScreenTitle(
                controller.readerState?.let { state ->
                    stringResource(MR.strings.reader_window_title, state.entryTitle, state.window.current.name)
                },
            )
            val state = controller.readerState
            val guideDue = bookInputGuideDue(preferences, shown = state != null)
            // Nothing but the book shows: no menu, and no sheet or dialog, which back would close
            val overBook = state != null && !state.chromeVisible && !controller.handlesBack &&
                controller.pendingHiddenText == null
            val pointer = rememberReaderPointer(hidesWhenStill = overBook && !guideDue)
            Box(modifier = Modifier.fillMaxSize().then(pointer)) {
                BookDocumentReaderContent(controller, host, dependencies)
                BookReaderKeys(controller, LocalWindowFullScreen.current, enabled = state != null && !guideDue)
                if (guideDue) BookReaderInputGuide(preferences)
            }
        }
    }

    private fun openWebView(
        context: PlatformContext,
        navigation: BookReaderNavigation,
        resolution: EntryChildWebViewResolution.Available,
        title: String?,
    ) {
        if (navigation.webViewAvailable) {
            navigation.openWebView(resolution.url, title, resolution.sourceId)
        } else {
            dependencies.linkOpener.open(context, resolution.url)
        }
    }
}

/** The chapter the reader opens, and the session the open flow prepared for it, claimed with [sessionToken]. */
private data class OpenedBook(val request: BookReaderRequest, val sessionToken: String?)

/** Set as the reader reopens for new text rules, which saved where it was already. */
private class ReopenFlag {
    var value = false
}

/**
 * Tells [controller] when the screen showing the reader comes and goes, as Android's activity does. Leaving the
 * screen saves where the reader is, unless [persistOnLeave] says it was saved already.
 */
@Composable
private fun FollowLifecycle(controller: BookDocumentReaderController, persistOnLeave: () -> Boolean) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> controller.onStart()
                Lifecycle.Event.ON_RESUME -> controller.onResume()
                Lifecycle.Event.ON_STOP -> controller.onStop(persist = true)
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                controller.onStop(persist = persistOnLeave())
            }
        }
    }
}
