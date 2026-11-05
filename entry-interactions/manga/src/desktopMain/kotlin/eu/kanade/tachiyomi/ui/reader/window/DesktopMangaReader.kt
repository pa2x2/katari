package eu.kanade.tachiyomi.ui.reader.window

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.DEFAULT_ARGS_KEY
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.savedstate.savedState
import dev.zacsweers.metro.Inject
import dev.zacsweers.metrox.viewmodel.LocalMetroViewModelFactory
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.screen.MangaReaderController
import eu.kanade.tachiyomi.ui.reader.screen.MangaReaderScreen
import eu.kanade.tachiyomi.ui.reader.screen.MangaReaderScreenHost
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderPages
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderViewer
import eu.kanade.tachiyomi.ui.reader.viewer.viewer
import mihon.core.platform.PlatformContext
import mihon.core.platform.currentPlatformContext
import mihon.core.platform.link.LinkOpener
import mihon.core.platform.power.ScreenIdleInhibitor
import mihon.entry.interactions.manga.reader.settings.MangaReaderPreferences
import mihon.entry.interactions.reader.window.MangaReader
import mihon.entry.interactions.reader.window.MangaReaderNavigation
import mihon.entry.interactions.reader.window.MangaReaderRequest
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.viewer.chrome.ui.window.KeepScreenAwake
import mihon.entry.viewer.chrome.ui.window.ViewerFullScreen
import mihon.entry.viewer.settings.shared.ReaderSharedSettingsRegistry
import mihon.presentation.kit.window.LocalWindowFullScreen
import mihon.presentation.kit.window.ReportScreenTitle
import tachiyomi.core.common.preference.PreferenceStore
import tachiyomi.i18n.*
import tachiyomi.presentation.core.i18n.stringResource

/**
 * The desktop's [MangaReader]: the shared reader screen over the shared viewers, as Android's reader activity hosts
 * them. It takes the keyboard ([MangaReaderKeys]), hides the mouse pointer while it rests over the pages
 * ([rememberReaderPointer]), offers the window's full screen, keeps the screen awake as the reader's setting says, and
 * shows a guide to the mouse and keys the first time it opens in a profile ([MangaReaderInputGuide]). While it fills
 * the window, the window's title names the series and chapter. There is no text recognition on the desktop, so the
 * text features stay out.
 */
@Inject
internal class DesktopMangaReader(
    private val sharedSettingsRegistry: ReaderSharedSettingsRegistry,
    preferenceStore: PreferenceStore,
    private val linkOpener: LinkOpener,
    private val screenIdleInhibitor: ScreenIdleInhibitor,
) : MangaReader {
    private val preferences = MangaReaderPreferences(preferenceStore)

    @Composable
    override fun Content(request: MangaReaderRequest, navigation: MangaReaderNavigation) {
        val viewModel = readerViewModel(request)
        val scope = rememberCoroutineScope()
        val controller = remember(viewModel) { MangaReaderController(viewModel, scope, overlayOrigin = { 0f to 0f }) }
        DisposableEffect(controller) {
            onDispose(controller::destroy)
        }
        FollowLifecycle(controller)

        val state by viewModel.state.collectAsState()
        val readingMode by viewModel.effectiveReadingMode.collectAsState()
        val loaded = state.manga != null
        val context = currentPlatformContext()
        val latestNavigation by rememberUpdatedState(navigation)
        var viewer by remember(controller) { mutableStateOf<ReaderViewer?>(null) }
        LaunchedEffect(controller, readingMode, loaded) {
            if (!loaded) return@LaunchedEffect
            // A page that failed to load offers its address, which the desktop opens in the browser
            val next = readingMode.viewer(controller) { url, _ -> linkOpener.open(context, url) }
            controller.setViewer(next)
            viewer = next
        }

        val host = remember(controller, context) { screenHost(context, viewModel, latestNavigation) }
        ReportScreenTitle(
            state.manga?.displayTitle?.let { series ->
                val chapter = state.currentChapter?.chapter?.name
                if (chapter == null) series else stringResource(MR.strings.reader_window_title, series, chapter)
            },
        )
        val settings = viewModel.settingsBindings.collectAsState().value
        val keepScreenOn = settings?.keepScreenOn?.state?.collectAsState()?.value?.effectiveValue == true
        KeepScreenAwake(
            active = keepScreenOn,
            reason = stringResource(MR.strings.reader_keeps_screen_awake),
            inhibitor = screenIdleInhibitor,
        )
        val fullScreen = LocalWindowFullScreen.current
        val shown = viewer
        val guideDue = inputGuideDue(preferences, shown = shown != null)
        val pointer = rememberReaderPointer(hidesWhenStill = !state.menuVisible && state.dialog == null && !guideDue)
        ViewerFullScreen(fullScreen) {
            Box(modifier = Modifier.fillMaxSize().then(pointer)) {
                if (shown != null && settings != null) ReaderPages(shown, settings, tapZonesHeld = guideDue)
                MangaReaderScreen(controller, host)
                MangaReaderKeys(controller, fullScreen, enabled = shown != null && state.dialog == null)
                if (guideDue) MangaReaderInputGuide(preferences)
            }
        }
    }

    private fun screenHost(
        context: PlatformContext,
        viewModel: ReaderViewModel,
        navigation: MangaReaderNavigation,
    ) = MangaReaderScreenHost(
        context = context,
        sharedSettingsRegistry = sharedSettingsRegistry,
        webViewActions = buildList {
            if (navigation.webViewAvailable) add(EntryChildWebViewAction.OPEN_IN_WEB_VIEW)
            add(EntryChildWebViewAction.OPEN_IN_BROWSER)
        },
        hasDisplayCutout = { false },
        onNavigateUp = navigation::close,
        onClose = navigation::close,
        onOpenEntry = { viewModel.manga?.id?.let(navigation::openEntry) },
        onOpenWebView = { action, resolution ->
            when (action) {
                EntryChildWebViewAction.OPEN_IN_WEB_VIEW ->
                    navigation.openWebView(resolution.url, viewModel.manga?.displayTitle, resolution.sourceId)
                EntryChildWebViewAction.OPEN_IN_BROWSER -> linkOpener.open(context, resolution.url)
                EntryChildWebViewAction.SHARE -> error("The desktop reader offers no sharing")
            }
        },
        onOpenDefaultSettings = navigation::openReaderSettings,
        textLayer = null,
    )
}

/**
 * The reader's ViewModel for [request], kept by the screen showing the reader, with the arguments Android's reader
 * activity gets from its intent.
 */
@Composable
private fun readerViewModel(request: MangaReaderRequest): ReaderViewModel {
    val owner =
        checkNotNull(LocalViewModelStoreOwner.current) { "The reader shows only in a screen that keeps ViewModels" }
    val defaults =
        (owner as? HasDefaultViewModelProviderFactory)?.defaultViewModelCreationExtras ?: CreationExtras.Empty
    val extras = MutableCreationExtras(defaults).apply {
        set(
            DEFAULT_ARGS_KEY,
            savedState {
                putLong("manga", request.entryId)
                putLong("chapter", request.chapterId)
                request.pageIndex?.let { putInt("page", it) }
            },
        )
    }
    return viewModel(viewModelStoreOwner = owner, factory = LocalMetroViewModelFactory.current, extras = extras)
}

/** Tells [controller] when the screen showing the reader comes and goes, as Android's activity does. */
@Composable
private fun FollowLifecycle(controller: MangaReaderController) {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, controller) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> controller.onResumed()
                Lifecycle.Event.ON_PAUSE -> controller.onPaused()
                Lifecycle.Event.ON_STOP -> controller.onStopped()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) controller.onPaused()
        }
    }
}
