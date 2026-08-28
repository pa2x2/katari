package mihon.entry.interactions.book.document.reader

import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import eu.kanade.tachiyomi.util.system.toast
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderSettingBindings
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderSettingsProvider
import mihon.entry.interactions.book.document.reader.settings.BookDocumentReaderThemeMode
import mihon.entry.interactions.book.navigation.BookChapterNavigationResolver
import mihon.entry.interactions.book.navigation.BookChapterReadingOrder
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.interactions.book.reader.BookChildWebViewResolver
import mihon.entry.interactions.book.reader.BookReaderErrorScreen
import mihon.entry.interactions.book.reader.BookReaderLoadingScreen
import mihon.entry.interactions.book.reader.BookReaderOpenResult
import mihon.entry.interactions.book.reader.BookReaderSessionFactory
import mihon.entry.interactions.book.reader.BookReaderSessionRegistry
import mihon.entry.interactions.book.reader.OpenedBookReaderSession
import mihon.entry.interactions.book.reader.navigation.BookReaderNavigationPresenter
import mihon.entry.interactions.book.reader.selection.BookSelectionActionCoordinator
import mihon.entry.interactions.book.reader.speech.BookShortFormSpeechController
import mihon.entry.interactions.book.reader.speech.BookShortFormSpeechFailure
import mihon.entry.interactions.book.reader.translation.BookSelectionTranslationController
import mihon.entry.interactions.child.EntryChildListFeature
import mihon.entry.interactions.runtime.EntryInteractionActivity
import mihon.entry.interactions.runtime.registerEntryInteractionSecureScreen
import mihon.entry.interactions.runtime.setEntryInteractionContent
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.interactions.source.EntryWebViewFeature
import mihon.entry.interactions.source.launchEntryChildWebViewAction
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.navigation.openViewerSettings
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import mihon.tts.api.TtsFeature
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Native endless-scroll host for semantic BOOK documents. */
internal class BookDocumentReaderActivity : EntryInteractionActivity() {
    private val retainedSessions by viewModels<BookDocumentReaderSessionViewModel>()
    private var surfaceState by mutableStateOf<BookDocumentReaderSurfaceState>(BookDocumentReaderSurfaceState.Loading)
    private var readerState by mutableStateOf<BookDocumentReaderState?>(null)
    private var selectionCoordinator: BookSelectionActionCoordinator? = null
    private val selectionActionModeAvoidance = BookSelectionActionModeAvoidance()
    private var settingBindings: BookDocumentReaderSettingBindings? = null
    private val snackbarHostState = SnackbarHostState()
    private lateinit var childWebViewResolver: BookChildWebViewResolver
    private lateinit var chapterCoordinator: BookDocumentChapterCoordinator
    private lateinit var readerSystemBars: BookDocumentReaderSystemBars
    private var startupJob: Job? = null
    private var navigationPresentationJob: Job? = null
    private val navigationPresenter by lazy {
        BookReaderNavigationPresenter(
            getEntryWithChapters = Injekt.get<GetEntryWithChapters>(),
            childListFeature = Injekt.get<EntryChildListFeature>(),
        )
    }
    private val startupRequest by lazy {
        BookReaderRequest(
            entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L),
            chapterId = intent.getLongExtra(EXTRA_CHAPTER_ID, -1L),
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        readerSystemBars = BookDocumentReaderSystemBars(window)
        registerEntryInteractionSecureScreen()
        childWebViewResolver = BookChildWebViewResolver(
            scope = lifecycleScope,
            feature = Injekt.get<EntryWebViewFeature>(),
            currentChapterId = { retainedSessions.currentChapterId },
            onResolution = { resolution -> readerState = readerState?.copy(childWebView = resolution) },
            onFailure = { error ->
                logcat(LogPriority.ERROR, error) { "Failed to resolve BOOK child WebView URL" }
            },
        )
        chapterCoordinator = BookDocumentChapterCoordinator(
            context = this,
            scope = lifecycleScope,
            retainedSessions = retainedSessions,
            sessionFactory = Injekt.get<BookReaderSessionFactory>(),
            isNextChapterPreparationEnabled = {
                settingBindings?.prepareNextChapter?.state?.value?.effectiveValue == true
            },
            currentState = { readerState },
            updateState = { readerState = it },
            onSessionActivated = { session ->
                selectionCoordinator?.updateCapabilities(session.readerCapabilities)
                childWebViewResolver.resolve(session)
            },
        )
        val readerContent = ComposeView(this)
        setContentView(
            BookSelectionActionModeHost(this, selectionActionModeAvoidance).apply {
                addView(
                    readerContent,
                    ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT,
                    ),
                )
            },
        )
        readerContent.setEntryInteractionContent {
            when (val surface = surfaceState) {
                BookDocumentReaderSurfaceState.Loading -> BookReaderLoadingScreen(
                    contentDescription = getString(R.string.book_reader_loading),
                )
                is BookDocumentReaderSurfaceState.Error -> BookReaderErrorScreen(
                    title = getString(R.string.book_reader_unavailable_title),
                    message = surface.message,
                    closeLabel = getString(R.string.book_reader_close),
                    onRetry = ::retryOpen.takeIf { surface.canRetry },
                    onClose = ::finish,
                )
                BookDocumentReaderSurfaceState.Ready -> readerState?.let { state ->
                    BookDocumentReaderScreen(
                        state = state,
                        settingBindings = requireNotNull(settingBindings),
                        selectionCoordinator = selectionCoordinator,
                        onLocation = chapterCoordinator::onLocation,
                        onTransitionReached = { chapterCoordinator.loadChapter(it, activate = false, retry = true) },
                        onTerminalObservation = chapterCoordinator::onTerminalObservation,
                        onChapterSelected = ::selectChapterFromNavigation,
                        onChromeToggle = ::toggleChrome,
                        onChromeHide = { setChromeVisible(false) },
                        onUserScrollStarted = chapterCoordinator::onUserScrollStarted,
                        onNavigationVisibilityChange = ::setNavigationVisible,
                        onSettingsVisibilityChange = { visible ->
                            readerState = readerState?.copy(settingsVisible = visible)
                        },
                        onOpenDefaultSettings = {
                            openViewerSettings(BookDocumentReaderSettingsProvider.PROVIDER_ID)
                        },
                        onChildWebViewAction = ::launchChildWebViewAction,
                        snackbarHostState = snackbarHostState,
                        onAnchorMissing = { showReaderFeedback(R.string.book_document_anchor_missing) },
                        onExternalLinkClick = ::launchExternalLink,
                        onTranslationPopupBoundsChanged = selectionActionModeAvoidance::updateBounds,
                        onClose = ::finish,
                    )
                }
            }
        }
        if (startupRequest.entryId < 0L || startupRequest.chapterId < 0L) {
            showError(getString(R.string.book_reader_invalid_request))
        } else {
            startOpen()
        }
    }

    override fun onStart() {
        super.onStart()
        chapterCoordinator.startReading()
    }

    override fun onResume() {
        super.onResume()
        selectionCoordinator?.onResume()
    }

    override fun onStop() {
        selectionCoordinator?.onStop()
        chapterCoordinator.stopReading(persist = !isChangingConfigurations)
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && surfaceState == BookDocumentReaderSurfaceState.Ready) {
            applyReaderSystemBars()
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        readerState?.childWebView?.url?.let { outContent.webUri = Uri.parse(it) }
    }

    override fun onDestroy() {
        navigationPresentationJob?.cancel()
        selectionActionModeAvoidance.clear()
        chapterCoordinator.close()
        childWebViewResolver.close()
        selectionCoordinator?.close()
        selectionCoordinator = null
        super.onDestroy()
    }

    private suspend fun installSettingBindings(entryId: Long) {
        if (settingBindings != null) return
        val bindings = BookDocumentReaderSettingBindings.create(
            provider = Injekt.get<BookDocumentReaderSettingsProvider>(),
            binder = Injekt.get<ViewerSettingBinder>(),
            entryId = entryId,
        )
        settingBindings = bindings
        lifecycleScope.launch {
            bindings.keepScreenAlive.state
                .map { setting -> setting.effectiveValue }
                .distinctUntilChanged()
                .collect(::setKeepScreenAlive)
        }
        lifecycleScope.launch {
            bindings.prepareNextChapter.state
                .map { setting -> setting.effectiveValue }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) chapterCoordinator.prepareCurrentNextChapterIfNeeded()
                }
        }
        lifecycleScope.launch {
            bindings.showStatusBar.state
                .map { setting -> setting.effectiveValue }
                .distinctUntilChanged()
                .collect { applyReaderSystemBars() }
        }
        lifecycleScope.launch {
            bindings.showNavigationBar.state
                .map { setting -> setting.effectiveValue }
                .distinctUntilChanged()
                .collect { applyReaderSystemBars() }
        }
        lifecycleScope.launch {
            bindings.themeMode.state
                .map { setting -> setting.effectiveValue }
                .distinctUntilChanged()
                .collect { applyReaderSystemBars() }
        }
    }

    private fun retryOpen() {
        val error = surfaceState as? BookDocumentReaderSurfaceState.Error ?: return
        if (!error.canRetry) return
        startOpen()
    }

    private fun startOpen() {
        startupJob?.cancel()
        navigationPresentationJob?.cancel()
        surfaceState = BookDocumentReaderSurfaceState.Loading
        startupJob = lifecycleScope.launch {
            try {
                open()
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                logcat(LogPriority.ERROR, error) { "Unexpected BOOK document reader startup failure" }
                showError(error.message ?: "The book reader could not be opened.", canRetry = true)
            }
        }
    }

    private suspend fun open() {
        installSettingBindings(startupRequest.entryId)
        val retained = retainedSessions.currentSession()
        val token = intent.getStringExtra(EXTRA_SESSION_TOKEN)
        val handedOff = if (retained == null && !token.isNullOrBlank()) {
            Injekt.get<BookReaderSessionRegistry>().claim(token, startupRequest)
        } else {
            null
        }
        val result = when (val existing = retained ?: handedOff) {
            null -> Injekt.get<BookReaderSessionFactory>().open(
                this,
                startupRequest,
                BookDocumentReaderProcessor.PROCESSOR_ID,
            )
            else -> BookReaderOpenResult.Success(existing)
        }
        when (result) {
            is BookReaderOpenResult.Failure -> showError(result.failure.message, result.canRetry)
            is BookReaderOpenResult.Success -> {
                if (retained == null) retainedSessions.attachInitial(result.session)
                showInitialSession(result.session)
            }
        }
    }

    private suspend fun showInitialSession(session: OpenedBookReaderSession) {
        val readingOrder = BookChapterReadingOrder(
            Injekt.get<BookChapterNavigationResolver>().resolveAll(session.entry),
        )
        val window = readingOrder.window(session.chapter.id)
            ?: return showError(getString(R.string.book_document_chapter_missing))
        val section = session.toDocumentSection(retainedSessions.locator(session.chapter.id))
            ?: return showError(getString(R.string.book_document_incompatible))
        ensureSelectionCoordinator(session)
        val progression = section.document.document.progressionAt(section.initialPosition)
        readerState = BookDocumentReaderState(
            entryTitle = session.entry.displayTitle,
            readingOrder = readingOrder,
            currentChapterId = session.chapter.id,
            window = window,
            loadedSections = mapOf(session.chapter.id to section),
            visualChapterProgression = progression,
        )
        childWebViewResolver.resolve(session)
        surfaceState = BookDocumentReaderSurfaceState.Ready
        setChromeVisible(false)
        chapterCoordinator.startReading()
        chapterCoordinator.prepareNextChapterIfNeeded(progression.toDouble())
    }

    private fun setNavigationVisible(visible: Boolean) {
        readerState = readerState?.copy(navigationVisible = visible)
        if (!visible) {
            navigationPresentationJob?.cancel()
            navigationPresentationJob = null
            return
        }
        val entry = retainedSessions.currentSession()?.entry ?: return
        val readingOrder = readerState?.readingOrder ?: return
        observeNavigationPresentation(entry, readingOrder)
    }

    private fun observeNavigationPresentation(entry: Entry, readingOrder: BookChapterReadingOrder) {
        navigationPresentationJob?.cancel()
        navigationPresentationJob = lifecycleScope.launch {
            navigationPresenter.observe(entry, readingOrder.chapters)
                .catch { error ->
                    logcat(LogPriority.ERROR, error) { "Failed to observe BOOK table-of-contents state" }
                }
                .collect { presentation ->
                    readerState = readerState?.copy(navigationPresentation = presentation)
                }
        }
    }

    private fun ensureSelectionCoordinator(session: OpenedBookReaderSession) {
        if (session.readerSettingsSurfaceId != BookDocumentReaderProcessor.SETTINGS_SURFACE_ID) return
        val automaticTranslation = settingBindings?.automaticTranslation ?: return
        val translationController = BookSelectionTranslationController(
            feature = Injekt.get<TranslationFeature>(),
            hostActions = Injekt.get<TranslationHostActions>(),
            automaticSelectionSetting = automaticTranslation.state,
            scope = lifecycleScope,
            initialCapabilities = session.readerCapabilities,
        )
        selectionCoordinator = BookSelectionActionCoordinator(
            translationController = translationController,
            speechController = BookShortFormSpeechController(
                feature = Injekt.get<TtsFeature>(),
                scope = lifecycleScope,
                onFailure = ::showSpeechFailure,
            ),
            scope = lifecycleScope,
            initialCapabilities = session.readerCapabilities,
        )
    }

    private fun showSpeechFailure(failure: BookShortFormSpeechFailure) {
        val message = when (failure) {
            BookShortFormSpeechFailure.LanguageUnavailable ->
                R.string.book_reader_selection_speech_language_unavailable
            BookShortFormSpeechFailure.ConfigurationRequired ->
                R.string.book_reader_selection_speech_configuration_required
            BookShortFormSpeechFailure.Unavailable ->
                R.string.book_reader_selection_speech_unavailable
            BookShortFormSpeechFailure.PlaybackFailed ->
                R.string.book_reader_selection_speech_failed
        }
        toast(getString(message))
    }

    private fun launchChildWebViewAction(
        action: EntryChildWebViewAction,
        resolution: EntryChildWebViewResolution.Available,
    ) {
        launchEntryChildWebViewAction(action, resolution, retainedSessions.currentSession()?.entry?.displayTitle)
            .onFailure { error -> logcat(LogPriority.ERROR, error) { "Failed to launch BOOK source action" } }
    }

    private fun launchExternalLink(url: String) {
        runCatching {
            val uri = Uri.parse(url)
            require(uri.scheme.equals("http", true) || uri.scheme.equals("https", true)) {
                "Unsupported external link"
            }
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure { error ->
            logcat(LogPriority.ERROR, error) { "Failed to launch prose external link" }
            showReaderFeedback(R.string.book_document_external_link_unavailable)
        }
    }

    private fun showReaderFeedback(@StringRes messageRes: Int) {
        lifecycleScope.launch {
            snackbarHostState.showSnackbar(getString(messageRes))
        }
    }

    private fun selectChapterFromNavigation(chapter: EntryChapter) {
        window.decorView.findFocus()?.clearFocus()
        chapterCoordinator.selectChapter(chapter, retry = true)
    }

    private fun setChromeVisible(visible: Boolean) {
        if (readerState?.chromeVisible != visible) {
            readerState = readerState?.copy(chromeVisible = visible)
        }
        applyReaderSystemBars()
    }

    private fun toggleChrome() {
        setChromeVisible(readerState?.chromeVisible != true)
    }

    private fun setKeepScreenAlive(enabled: Boolean) {
        if (enabled) {
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    private fun applyReaderSystemBars() {
        if (surfaceState != BookDocumentReaderSurfaceState.Ready) return
        val chromeVisible = readerState?.chromeVisible == true
        val keepStatusBarVisible = settingBindings?.showStatusBar?.state?.value?.effectiveValue == true
        val keepNavigationBarVisible = settingBindings?.showNavigationBar?.state?.value?.effectiveValue == true
        val readerTheme = settingBindings?.themeMode?.state?.value?.effectiveValue ?: BookDocumentReaderThemeMode.APP
        readerSystemBars.apply(
            chromeVisible = chromeVisible,
            keepStatusBarVisible = keepStatusBarVisible,
            keepNavigationBarVisible = keepNavigationBarVisible,
            readerTheme = readerTheme,
        )
    }

    private fun showError(message: String, canRetry: Boolean = false) {
        readerSystemBars.showAppBars()
        surfaceState = BookDocumentReaderSurfaceState.Error(message, canRetry)
    }

    companion object {
        private const val EXTRA_ENTRY_ID = "entry_id"
        private const val EXTRA_CHAPTER_ID = "chapter_id"
        private const val EXTRA_SESSION_TOKEN = "session_token"
        fun newIntent(
            context: Context,
            request: BookReaderRequest,
            sessionToken: String,
        ): Intent = Intent(context, BookDocumentReaderActivity::class.java).apply {
            putExtra(EXTRA_ENTRY_ID, request.entryId)
            putExtra(EXTRA_CHAPTER_ID, request.chapterId)
            putExtra(EXTRA_SESSION_TOKEN, sessionToken)
        }
    }
}

private sealed interface BookDocumentReaderSurfaceState {
    data object Loading : BookDocumentReaderSurfaceState
    data object Ready : BookDocumentReaderSurfaceState
    data class Error(
        val message: String,
        val canRetry: Boolean,
    ) : BookDocumentReaderSurfaceState
}
