package mihon.entry.interactions.book.document.reader

import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.navigation.BookChapterNavigationResolver
import mihon.entry.interactions.book.processor.BookReaderRequest
import mihon.entry.interactions.book.reader.BookChildWebViewResolver
import mihon.entry.interactions.book.reader.BookReaderErrorScreen
import mihon.entry.interactions.book.reader.BookReaderLoadingScreen
import mihon.entry.interactions.book.reader.BookReaderOpenResult
import mihon.entry.interactions.book.reader.BookReaderSessionFactory
import mihon.entry.interactions.book.reader.BookReaderSessionRegistry
import mihon.entry.interactions.book.reader.OpenedBookReaderSession
import mihon.entry.interactions.book.reader.translation.BookAutomaticTranslationSettingsProvider
import mihon.entry.interactions.book.reader.translation.BookSelectionTranslationController
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.runtime.EntryInteractionActivity
import mihon.entry.interactions.runtime.registerEntryInteractionSecureScreen
import mihon.entry.interactions.runtime.setEntryInteractionContent
import mihon.entry.interactions.source.EntryChildWebViewAction
import mihon.entry.interactions.source.EntryChildWebViewResolution
import mihon.entry.interactions.source.EntryWebViewFeature
import mihon.entry.interactions.source.launchEntryChildWebViewAction
import mihon.entry.interactions.viewer.entryChildWindow
import mihon.translation.api.TranslationFeature
import mihon.translation.api.host.TranslationHostActions
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.EntryChapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Native endless-scroll host for semantic BOOK documents. */
internal class BookDocumentReaderActivity : EntryInteractionActivity() {
    private val retainedSessions by viewModels<BookDocumentReaderSessionViewModel>()
    private var surfaceState by mutableStateOf<BookDocumentReaderSurfaceState>(BookDocumentReaderSurfaceState.Loading)
    private var readerState by mutableStateOf<BookDocumentReaderState?>(null)
    private var translationController: BookSelectionTranslationController? = null
    private lateinit var childWebViewResolver: BookChildWebViewResolver
    private lateinit var chapterCoordinator: BookDocumentChapterCoordinator
    private val preparationPreferences by lazy { Injekt.get<ReaderChapterPreparationPreferences>() }

    private val windowInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        windowInsetsController.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
            preparationPreferences = preparationPreferences,
            currentState = { readerState },
            updateState = { readerState = it },
            onSessionActivated = { session ->
                translationController?.updateCapabilities(session.readerCapabilities)
                childWebViewResolver.resolve(session)
            },
        )
        setEntryInteractionContent {
            when (val surface = surfaceState) {
                BookDocumentReaderSurfaceState.Loading -> BookReaderLoadingScreen(
                    contentDescription = getString(R.string.book_reader_loading),
                )
                is BookDocumentReaderSurfaceState.Error -> BookReaderErrorScreen(
                    title = getString(R.string.book_reader_unavailable_title),
                    message = surface.message,
                    closeLabel = getString(R.string.book_reader_close),
                    onClose = ::finish,
                )
                BookDocumentReaderSurfaceState.Ready -> readerState?.let { state ->
                    BookDocumentReaderScreen(
                        state = state,
                        translationController = translationController,
                        onLocation = chapterCoordinator::onLocation,
                        onTransitionReached = { chapterCoordinator.loadChapter(it, activate = false, retry = true) },
                        onTerminalObservation = chapterCoordinator::onTerminalObservation,
                        onChapterSelected = ::selectChapterFromNavigation,
                        onChromeToggle = ::toggleChrome,
                        onChromeHide = { setChromeVisible(false) },
                        onNavigationVisibilityChange = { visible ->
                            readerState = readerState?.copy(navigationVisible = visible)
                        },
                        onSettingsVisibilityChange = { visible ->
                            readerState = readerState?.copy(settingsVisible = visible)
                        },
                        onChildWebViewAction = ::launchChildWebViewAction,
                        onExternalLinkClick = ::launchExternalLink,
                        onClose = ::finish,
                    )
                }
            }
        }
        lifecycleScope.launch { open() }
        lifecycleScope.launch {
            preparationPreferences.prepareNextChapter.changes().collect { enabled ->
                if (enabled) chapterCoordinator.prepareCurrentNextChapterIfNeeded()
            }
        }
    }

    override fun onStart() {
        super.onStart()
        chapterCoordinator.startReading()
    }

    override fun onResume() {
        super.onResume()
        translationController?.onResume()
    }

    override fun onStop() {
        chapterCoordinator.stopReading(persist = !isChangingConfigurations)
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && surfaceState == BookDocumentReaderSurfaceState.Ready) {
            setSystemBarsVisible(readerState?.chromeVisible == true)
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        readerState?.childWebView?.url?.let { outContent.webUri = Uri.parse(it) }
    }

    override fun onDestroy() {
        chapterCoordinator.close()
        childWebViewResolver.close()
        translationController?.close()
        translationController = null
        super.onDestroy()
    }

    private suspend fun open() {
        val request = BookReaderRequest(
            entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L),
            chapterId = intent.getLongExtra(EXTRA_CHAPTER_ID, -1L),
        )
        if (request.entryId < 0L || request.chapterId < 0L) {
            showError(getString(R.string.book_reader_invalid_request))
            return
        }
        val retained = retainedSessions.currentSession()
        val token = intent.getStringExtra(EXTRA_SESSION_TOKEN)
        val handedOff = if (retained == null && !token.isNullOrBlank()) {
            Injekt.get<BookReaderSessionRegistry>().claim(token, request)
        } else {
            null
        }
        val result = when (val existing = retained ?: handedOff) {
            null -> Injekt.get<BookReaderSessionFactory>().open(this, request, BookDocumentReaderProcessor.PROCESSOR_ID)
            else -> BookReaderOpenResult.Success(existing)
        }
        when (result) {
            is BookReaderOpenResult.Failure -> showError(result.failure.message)
            is BookReaderOpenResult.Success -> {
                if (retained == null) retainedSessions.attachInitial(result.session)
                showInitialSession(result.session)
            }
        }
    }

    private suspend fun showInitialSession(session: OpenedBookReaderSession) {
        val chapters = Injekt.get<BookChapterNavigationResolver>().resolveAll(session.entry)
        val window = chapters.entryChildWindow(session.chapter.id, EntryChapter::id)
            ?: return showError(getString(R.string.book_document_chapter_missing))
        val section = session.toDocumentSection(retainedSessions.locator(session.chapter.id))
            ?: return showError(getString(R.string.book_document_incompatible))
        ensureTranslationController(session)
        val progression = section.document.document.progressionAt(section.initialPosition)
        readerState = BookDocumentReaderState(
            entryTitle = session.entry.displayTitle,
            chapters = chapters,
            currentChapterId = session.chapter.id,
            window = window,
            loadedSections = mapOf(session.chapter.id to section),
            chapterProgression = progression,
        )
        childWebViewResolver.resolve(session)
        surfaceState = BookDocumentReaderSurfaceState.Ready
        setChromeVisible(false)
        chapterCoordinator.startReading()
        chapterCoordinator.prepareNextChapterIfNeeded(progression.toDouble())
    }

    private fun ensureTranslationController(session: OpenedBookReaderSession) {
        val settingsSurface = session.readerSettingsSurfaceId ?: return
        val preference = Injekt.get<BookAutomaticTranslationSettingsProvider>()
            .automaticSelectionEnabled(settingsSurface)
        translationController = BookSelectionTranslationController(
            feature = Injekt.get<TranslationFeature>(),
            hostActions = Injekt.get<TranslationHostActions>(),
            automaticSelectionEnabled = preference,
            scope = lifecycleScope,
            initialCapabilities = session.readerCapabilities,
        )
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
        setSystemBarsVisible(visible)
    }

    private fun toggleChrome() {
        setChromeVisible(readerState?.chromeVisible != true)
    }

    private fun setSystemBarsVisible(visible: Boolean) {
        if (visible) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun showError(message: String) {
        setSystemBarsVisible(true)
        surfaceState = BookDocumentReaderSurfaceState.Error(message)
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
    data class Error(val message: String) : BookDocumentReaderSurfaceState
}
