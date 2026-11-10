package eu.kanade.tachiyomi.ui.reader.screen

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import app.katari.entry.interactions.manga.reader.text.interaction.MangaReaderTextInteraction
import app.katari.entry.interactions.reader.settings.MangaReaderSettings
import app.katari.entry.interactions.reader.settings.ReadingMode
import app.katari.entry.interactions.source.EntryChildWebViewResolution
import app.katari.tts.ui.playback.ShortFormSpeechFailure
import dev.icerock.moko.resources.StringResource
import eu.kanade.presentation.reader.DisplayRefreshHost
import eu.kanade.tachiyomi.ui.reader.ReaderViewModel
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.viewer.ReaderViewer
import eu.kanade.tachiyomi.ui.reader.viewer.key.ReaderKeyCommand
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.i18n.*

/**
 * The reading session beside the view model, for as long as the reader's window is open: the viewer showing the pages,
 * auto scroll, moving between pages and chapters from the controls, the chapter's web page, the e-ink flashes, and the
 * text features' link to the pages on screen.
 *
 * Each platform's host makes one in its window's [scope], gives it the viewer for the reading mode through
 * [setViewer], forwards the viewer's and the window's events to it, and shows [notices].
 */
@Stable
internal class MangaReaderController(
    val viewModel: ReaderViewModel,
    private val scope: CoroutineScope,
    /** The window position of the layer that hosts the translation popup. */
    overlayOrigin: () -> Pair<Float, Float>,
    /** Called once the viewer has been given its chapters, such as to take down a loading indicator. */
    private val onChaptersSet: () -> Unit = {},
) {
    private val mutableViewer = MutableStateFlow<ReaderViewer?>(null)
    val viewer: StateFlow<ReaderViewer?> = mutableViewer.asStateFlow()

    private val noticeChannel = Channel<MangaReaderNotice>(Channel.BUFFERED)

    /** Short messages about what just happened, which the screen shows. */
    val notices: Flow<MangaReaderNotice> = noticeChannel.receiveAsFlow()

    var isAutoScrollRunning by mutableStateOf(false)
        private set

    private var autoScrollSpeed = MangaReaderSettings.AUTO_SCROLL_LEVEL_DEFAULT

    /** The current chapter's web page, once its source has resolved it. */
    var chapterWebView by mutableStateOf<EntryChildWebViewResolution.Available?>(null)
        private set

    /** Flashes the screen between pages for e-ink panels; null until the series' settings are loaded. */
    var displayRefresh by mutableStateOf<DisplayRefreshHost?>(null)
        private set

    /** Whether the slider or the return action is moving the viewer, which keeps the controls showing. */
    var isNavigatingFromControls = false
        private set

    private var checkpointJob: Job? = null

    val textInteraction = MangaReaderTextInteraction(
        session = viewModel.textSession,
        translation = viewModel.textTranslation,
        visibleSurfaces = { viewer.value?.visibleTextSurfaces().orEmpty() },
        preloadedSurfaces = { viewer.value?.preloadedTextSurfaces().orEmpty() },
        processAheadAllowed = viewModel::textProcessAheadAllowed,
        overlayOrigin = overlayOrigin,
        scope = scope,
    )

    init {
        val settings = viewModel.settingsBindings.filterNotNull().distinctUntilChanged()
        settings
            .onEach {
                displayRefresh = DisplayRefreshHost(
                    flashDurationMillis = it.flashDurationMillis,
                    flashColor = it.flashColor,
                    flashPageInterval = it.flashPageInterval,
                )
            }
            .launchIn(scope)
        settings
            .flatMapLatest { it.autoScrollSpeed.state.map { speed -> speed.effectiveValue } }
            .distinctUntilChanged()
            .onEach { speed ->
                autoScrollSpeed = speed
                if (isAutoScrollRunning) viewer.value?.updateAutoScrollSpeed(speed)
            }
            .launchIn(scope)
        settings
            .flatMapLatest { it.autoScrollEnabled.state.map { enabled -> enabled.effectiveValue } }
            .distinctUntilChanged()
            .drop(1)
            .onEach { enabled -> if (!enabled) stopAutoScroll(notify = false) }
            .launchIn(scope)
        viewModel.state
            .map { it.isLoadingAdjacentChapter }
            .distinctUntilChanged()
            .onEach { loading -> if (loading) viewModel.showLoadingDialog() else viewModel.closeDialog() }
            .launchIn(scope)
        viewModel.state
            .map { it.viewerChapters }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach(::setChapters)
            .launchIn(scope)
        viewModel.eventFlow
            .onEach(::onEvent)
            .launchIn(scope)
    }

    /**
     * Shows the pages in [newViewer] from now on, in place of the viewer before it, which is destroyed. The chapters
     * may have loaded before any viewer was there to take them, so the new viewer gets those already loaded.
     */
    fun setViewer(newViewer: ReaderViewer) {
        stopAutoScroll(notify = false)
        mutableViewer.value?.destroy()
        mutableViewer.value = newViewer
        viewModel.onViewerChanged()
        if (viewModel.readerSettings.showReadingMode.state.value.effectiveValue) {
            showNotice(ReadingMode.fromPreference(viewModel.getMangaReadingMode()).stringRes)
        }
        reloadChapters()
    }

    private fun reloadChapters() {
        viewModel.state.value.viewerChapters?.let(::setChapters)
    }

    private fun setChapters(viewerChapters: ViewerChapters) {
        viewer.value?.setChapters(viewerChapters)
        onChaptersSet()

        val chapterId = viewerChapters.current.chapter.id
        chapterWebView = null
        scope.launchIO {
            val resolution = viewModel.resolveChapterWebView()
            withUIContext {
                if (viewModel.state.value.currentChapter?.chapter?.id != chapterId) return@withUIContext
                when (resolution) {
                    is EntryChildWebViewResolution.Available -> chapterWebView = resolution
                    is EntryChildWebViewResolution.Failed ->
                        logcat(LogPriority.ERROR, resolution.cause) { "Failed to resolve chapter WebView URL" }
                    else -> Unit
                }
            }
        }
    }

    private fun showNotice(message: StringResource) {
        noticeChannel.trySend(MangaReaderNotice.Resource(message))
    }

    private fun showSpeechFailure(failure: ShortFormSpeechFailure) {
        failure.explanation?.let { explanation ->
            noticeChannel.trySend(MangaReaderNotice.Text(explanation))
            return
        }
        showNotice(
            when (failure) {
                ShortFormSpeechFailure.LanguageUnavailable -> MR.strings.reader_text_speech_language_unavailable
                ShortFormSpeechFailure.ConfigurationRequired -> MR.strings.reader_text_speech_configuration_required
                is ShortFormSpeechFailure.Unavailable -> MR.strings.reader_text_speech_unavailable
                is ShortFormSpeechFailure.PlaybackFailed -> MR.strings.reader_text_speech_failed
            },
        )
    }

    private fun onEvent(event: ReaderViewModel.Event) {
        when (event) {
            ReaderViewModel.Event.ReloadViewerChapters -> reloadChapters()
            ReaderViewModel.Event.PageChanged -> displayRefresh?.flash()
            ReaderViewModel.Event.StoredTranslationOpened -> {
                viewModel.textSession.setOverlay(true)
                textInteraction.setActive(true)
            }
            is ReaderViewModel.Event.TranslationSpeechFailed -> showSpeechFailure(event.failure)
            is ReaderViewModel.Event.SavedImage -> when (val result = event.result) {
                ReaderViewModel.SaveImageResult.Success -> showNotice(MR.strings.picture_saved)
                is ReaderViewModel.SaveImageResult.Error -> logcat(LogPriority.ERROR, result.error)
            }
            is ReaderViewModel.Event.SetCoverResult -> showNotice(
                when (event.result) {
                    ReaderViewModel.SetAsCoverResult.Success -> MR.strings.cover_updated
                    ReaderViewModel.SetAsCoverResult.AddToLibraryFirst -> MR.strings.notification_first_add_to_library
                    ReaderViewModel.SetAsCoverResult.Error -> MR.strings.notification_cover_update_failed
                },
            )
        }
    }

    fun onResumed() {
        viewModel.textSession.resume()
        viewModel.textTranslation.onResume()
        viewModel.startReadTimer()
        checkpointJob?.cancel()
        checkpointJob = scope.launchIO {
            while (isActive) {
                delay(HISTORY_CHECKPOINT_INTERVAL_MS)
                viewModel.checkpointHistory()
            }
        }
    }

    fun onPaused() {
        stopAutoScroll(notify = false)
        checkpointJob?.cancel()
        checkpointJob = null
        viewModel.saveChapterProgress()
        // The clock stops here rather than in the record, which a resume right after could otherwise overtake.
        val timeRead = viewModel.stopReadTimer()
        scope.launchNonCancellable { viewModel.recordTimeRead(timeRead) }
    }

    fun onStopped() {
        viewModel.textTranslationSpeech.stopPlayback()
    }

    /** Called when the reader's window closes for good. */
    fun destroy() {
        stopAutoScroll(notify = false)
        mutableViewer.value?.destroy()
    }

    fun setMenuVisibility(visible: Boolean) {
        viewModel.showMenus(visible)
    }

    fun toggleMenu() {
        setMenuVisibility(!viewModel.state.value.menuVisible)
    }

    fun moveToPageIndex(index: Int) {
        val viewer = viewer.value ?: return
        val page = viewModel.state.value.currentChapter?.pages?.getOrNull(index) ?: return
        viewer.moveToPage(page)
    }

    fun seekToPage(index: Int) {
        viewModel.jumpHistory.beginSeek(index)
        isNavigatingFromControls = true
        moveToPageIndex(index)
    }

    fun finishSeek() {
        isNavigatingFromControls = false
        viewModel.jumpHistory.finishSeek()
    }

    fun returnToPreviousPosition() {
        scope.launch {
            isNavigatingFromControls = true
            try {
                viewModel.returnToPreviousPosition()?.let(::moveToPageIndex)
            } finally {
                isNavigatingFromControls = false
            }
        }
    }

    fun goToProgressOffer() {
        scope.launch {
            isNavigatingFromControls = true
            try {
                viewModel.goToProgressOffer()?.let(::moveToPageIndex)
            } finally {
                isNavigatingFromControls = false
            }
        }
    }

    fun loadNextChapter() {
        scope.launch { if (viewModel.loadNextChapter()) moveToPageIndex(0) }
    }

    fun loadPreviousChapter() {
        scope.launch { if (viewModel.loadPreviousChapter()) moveToPageIndex(0) }
    }

    fun loadChapter(chapterId: Long) {
        scope.launch { viewModel.loadNavigationChapter(chapterId) }
    }

    fun toggleCropBorders() {
        showNotice(if (viewModel.toggleCropBorders()) MR.strings.on else MR.strings.off)
    }

    fun onPageShown(page: ReaderPage) {
        viewModel.onPageShown(page)
    }

    fun onPageSelected(page: ReaderPage, facing: ReaderPage? = null) {
        viewModel.onPageSelected(page, facing)
        textInteraction.refreshSurfaces()
    }

    /** Called from viewers when a page view starts or stops showing an image. */
    fun onPageTextSurfacesChanged() {
        textInteraction.refreshSurfaces()
    }

    /** Opens [page]'s actions: as a menu at [menuAt], a point of the window, or as a sheet when it is null. */
    fun onPageLongTap(page: ReaderPage, menuAt: Offset? = null) {
        viewModel.openPageDialog(page, menuAt)
    }

    /** Runs a key's [command]; the viewer turns or scrolls for the commands about pages. */
    fun onKeyCommand(command: ReaderKeyCommand) {
        when (command) {
            ReaderKeyCommand.NextChapter -> loadNextChapter()
            ReaderKeyCommand.PreviousChapter -> loadPreviousChapter()
            ReaderKeyCommand.ToggleMenu -> toggleMenu()
            ReaderKeyCommand.FirstPage -> moveToPageIndex(0)
            ReaderKeyCommand.LastPage -> {
                val pages = viewModel.state.value.currentChapter?.pages ?: return
                moveToPageIndex(pages.lastIndex)
            }
            ReaderKeyCommand.PageRight,
            ReaderKeyCommand.PageLeft,
            ReaderKeyCommand.NextPage,
            ReaderKeyCommand.PreviousPage,
            ReaderKeyCommand.ZoomIn,
            ReaderKeyCommand.ZoomOut,
            ReaderKeyCommand.ZoomFit,
            -> viewer.value?.onPageKey(command)
        }
    }

    /** Called when an active chapter transition requests its destination. */
    fun requestTransitionChapterLoad(chapter: ReaderChapter) {
        scope.launchIO { viewModel.loadChapterForTransition(chapter) }
    }

    /** Stops automatic movement when the viewer detects direct manipulation. */
    fun onViewerDirectManipulation() {
        stopAutoScroll(notify = false)
    }

    /** Whether auto scroll is turned on in the settings and the viewer can scroll by itself. */
    fun supportsAutoScroll(): Boolean {
        val enabled = viewModel.settingsBindings.value?.autoScrollEnabled?.state?.value?.effectiveValue ?: false
        return enabled && viewer.value?.supportsAutoScroll() == true
    }

    fun toggleAutoScroll() {
        if (isAutoScrollRunning) stopAutoScroll() else startAutoScroll()
    }

    private fun startAutoScroll() {
        if (!supportsAutoScroll() || isAutoScrollRunning) return
        viewer.value?.startAutoScroll(autoScrollSpeed)
        isAutoScrollRunning = true
        setMenuVisibility(false)
        showNotice(MR.strings.reader_auto_scroll_enabled)
    }

    private fun stopAutoScroll(notify: Boolean = true) {
        if (!isAutoScrollRunning) return
        viewer.value?.stopAutoScroll()
        isAutoScrollRunning = false
        if (notify) showNotice(MR.strings.reader_auto_scroll_disabled)
    }

    /** Moves running auto scroll's speed level by [delta]. */
    fun changeAutoScrollSpeed(delta: Int) {
        if (!isAutoScrollRunning) return
        val newSpeed = (autoScrollSpeed + delta).coerceIn(MangaReaderSettings.AUTO_SCROLL_SPEED_RANGE)
        if (newSpeed == autoScrollSpeed) return
        autoScrollSpeed = newSpeed
        viewer.value?.updateAutoScrollSpeed(newSpeed)
    }

    private companion object {
        const val HISTORY_CHECKPOINT_INTERVAL_MS = 30_000L
    }
}
