package mihon.entry.interactions.book.prose

import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.book.api.BookLocator
import mihon.book.api.document.BookDocumentPosition
import mihon.book.api.document.locatorAt
import mihon.book.api.document.resolvePosition
import mihon.entry.interactions.EntryChildWebViewAction
import mihon.entry.interactions.EntryChildWebViewResolution
import mihon.entry.interactions.EntryInteractionActivity
import mihon.entry.interactions.EntryWebViewFeature
import mihon.entry.interactions.book.BookAutomaticTranslationSettingsProvider
import mihon.entry.interactions.book.BookChapterNavigationResolver
import mihon.entry.interactions.book.BookChildWebViewResolver
import mihon.entry.interactions.book.BookReaderErrorScreen
import mihon.entry.interactions.book.BookReaderLoadingScreen
import mihon.entry.interactions.book.BookReaderOpenResult
import mihon.entry.interactions.book.BookReaderRequest
import mihon.entry.interactions.book.BookReaderSessionFactory
import mihon.entry.interactions.book.BookReaderSessionRegistry
import mihon.entry.interactions.book.BookSelectionTranslationController
import mihon.entry.interactions.book.OpenedBookReaderSession
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.displayName
import mihon.entry.interactions.book.document.reader.BookDocumentTextView
import mihon.entry.interactions.launchEntryChildWebViewAction
import mihon.entry.interactions.reader.preparation.ReaderChapterPreparationPreferences
import mihon.entry.interactions.setEntryInteractionContent
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationHostActions
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.EntryChapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Processor-owned reader surface for independently loaded HTML prose chapters. */
internal class HtmlProseChapterReaderActivity : EntryInteractionActivity() {
    internal val retainedSession by viewModels<HtmlProseReaderSessionViewModel>()
    private var surfaceState by mutableStateOf<ProseReaderSurfaceState>(ProseReaderSurfaceState.Loading)
    internal var uiState by mutableStateOf<HtmlProseReaderUiState?>(null)
    private var settings: HtmlProseSettingsBinding? = null
    internal var openedSession: OpenedBookReaderSession? = null
    internal var latestLocator: BookLocator? = null
    private lateinit var childWebViewResolver: BookChildWebViewResolver
    private lateinit var chapterCoordinator: HtmlProseChapterCoordinator
    internal var readingStartedAt: Long? = null
    private var pageLoaded = false
    private var translationController: BookSelectionTranslationController? = null

    private val windowInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }
    internal val chapterPreparationPreferences by lazy {
        Injekt.get<ReaderChapterPreparationPreferences>()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(null)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        windowInsetsController.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        childWebViewResolver = BookChildWebViewResolver(
            scope = lifecycleScope,
            feature = Injekt.get<EntryWebViewFeature>(),
            currentChapterId = { openedSession?.chapter?.id },
            onResolution = { resolution ->
                uiState = uiState?.copy(childWebView = resolution)
            },
            onFailure = { error ->
                logcat(LogPriority.ERROR, error) { "Failed to resolve BOOK child WebView URL" }
            },
        )
        chapterCoordinator = HtmlProseChapterCoordinator(
            scope = lifecycleScope,
            retainedSession = retainedSession,
            currentSession = { openedSession },
            currentState = { uiState },
            onStateChange = { uiState = it },
            prepareNextChapterEnabled = { chapterPreparationPreferences.prepareNextChapter.get() },
            resolveChapters = { session ->
                Injekt.get<BookChapterNavigationResolver>().resolveAll(session.entry)
            },
            openSession = ::openChapterSession,
            beforeSwitch = ::persistBeforeChapterSwitch,
            onSessionActivated = ::showSession,
            incompatibleSessionMessage = { getString(R.string.prose_reader_incompatible_session) },
        )
        setEntryInteractionContent {
            when (val state = surfaceState) {
                ProseReaderSurfaceState.Loading -> BookReaderLoadingScreen(
                    contentDescription = getString(R.string.book_reader_loading),
                )
                is ProseReaderSurfaceState.Error -> BookReaderErrorScreen(
                    title = getString(R.string.book_reader_unavailable_title),
                    message = state.message,
                    closeLabel = getString(R.string.book_reader_close),
                    onClose = ::finish,
                )
                ProseReaderSurfaceState.Ready -> {
                    val readerState = uiState
                    val readerSettings = settings
                    if (readerState != null && readerSettings != null) {
                        HtmlProseReaderScreen(
                            state = readerState,
                            settings = readerSettings,
                            onLocation = ::updateLocation,
                            onChapterEntered = chapterCoordinator::enterChapter,
                            onClose = ::finish,
                            onMenuVisibilityChange = ::setMenuVisibility,
                            onChapterListVisibilityChange = { visible ->
                                uiState = uiState?.copy(chapterListVisible = visible)
                            },
                            onChapterSelected = chapterCoordinator::selectChapter,
                            onTransitionChapterRequested = {
                                chapterCoordinator.requestTransitionChapter(it, retry = false)
                            },
                            onTransitionChapterRetry = {
                                chapterCoordinator.requestTransitionChapter(it, retry = true)
                            },
                            onSettingsVisibilityChange = { visible ->
                                uiState = uiState?.copy(settingsVisible = visible)
                            },
                            onChildWebViewAction = ::launchChildWebViewAction,
                            onExternalLinkClick = ::launchExternalLink,
                            translationController = translationController,
                        )
                    }
                }
            }
        }
        lifecycleScope.launch { open() }
        lifecycleScope.launch {
            chapterPreparationPreferences.prepareNextChapter.changes().collect { enabled ->
                if (enabled) {
                    chapterCoordinator.prepareNextChapterIfNeeded(latestLocator?.progression)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (openedSession != null && readingStartedAt == null) {
            readingStartedAt = SystemClock.elapsedRealtime()
        }
    }

    override fun onResume() {
        super.onResume()
        translationController?.onResume()
    }

    override fun onStop() {
        val elapsed = readingStartedAt
            ?.let { (SystemClock.elapsedRealtime() - it).coerceAtLeast(0L) }
            ?: 0L
        readingStartedAt = null
        persist(elapsed, persistLocation = !isChangingConfigurations)
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && surfaceState == ProseReaderSurfaceState.Ready) {
            setMenuVisibility(uiState?.menuVisible == true)
        }
    }

    override fun onDestroy() {
        chapterCoordinator.close()
        childWebViewResolver.close()
        translationController?.close()
        translationController = null
        openedSession = null
        settings = null
        super.onDestroy()
    }

    private suspend fun open() {
        val retained = retainedSession.currentSession
        val request = retained?.let { BookReaderRequest(it.entry.id, it.chapter.id) } ?: BookReaderRequest(
            entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L),
            chapterId = intent.getLongExtra(EXTRA_CHAPTER_ID, -1L),
        )
        val processorId = intent.getStringExtra(EXTRA_PROCESSOR_ID)
        chapterCoordinator.processorId = processorId
        val sessionToken = intent.getStringExtra(EXTRA_SESSION_TOKEN)
        if (request.entryId < 0L || request.chapterId < 0L || processorId.isNullOrBlank()) {
            showError(getString(R.string.book_reader_invalid_request))
            return
        }

        val handedOff = if (retained == null && !sessionToken.isNullOrBlank()) {
            Injekt.get<BookReaderSessionRegistry>().claim(sessionToken, request)
        } else {
            null
        }
        val result = when (val session = retained ?: handedOff) {
            null -> Injekt.get<BookReaderSessionFactory>().open(this, request, processorId)
            else -> BookReaderOpenResult.Success(session)
        }
        when (result) {
            is BookReaderOpenResult.Failure -> showError(
                getString(
                    R.string.book_reader_unavailable_message,
                    result.failure.reason.displayName(),
                    result.failure.message,
                ),
            )
            is BookReaderOpenResult.Success -> {
                if (retained == null) {
                    retainedSession.attachInitial(result.session)
                }
                showSession(result.session)
            }
        }
    }

    private suspend fun openChapterSession(
        chapter: EntryChapter,
        processorId: String,
    ): BookReaderOpenResult {
        val session = openedSession
            ?: return BookReaderOpenResult.Failure(
                mihon.book.api.BookFailure(
                    mihon.book.api.BookFailureReason.CONTENT_UNAVAILABLE,
                    getString(R.string.prose_reader_incompatible_session),
                ),
            )
        return Injekt.get<BookReaderSessionFactory>().open(
            this,
            BookReaderRequest(session.entry.id, chapter.id),
            processorId,
        )
    }

    private suspend fun persistBeforeChapterSwitch(completed: Boolean) {
        val session = openedSession ?: return
        val elapsed = readingStartedAt
            ?.let { (SystemClock.elapsedRealtime() - it).coerceAtLeast(0L) }
            ?: 0L
        readingStartedAt = null
        latestLocator?.let { session.saveLocation(it, completed = completed) }
        session.recordHistory(elapsed)
    }

    internal suspend fun showSession(
        session: OpenedBookReaderSession,
        resetViewer: Boolean = false,
    ) {
        val content = session.publicationSession as? HtmlProseChapterSession
        if (content == null) {
            translationController?.clearSelection()
            retainedSession.release()
            showError(getString(R.string.prose_reader_incompatible_session))
            return
        }
        try {
            val settingsSurfaceId = requireNotNull(session.readerSettingsSurfaceId) {
                "The prose reader session has no viewer settings surface"
            }
            val automaticTranslationSettings = Injekt.get<BookAutomaticTranslationSettingsProvider>()
            val activeTranslationController = translationController ?: BookSelectionTranslationController(
                feature = Injekt.get<TranslationFeature>(),
                hostActions = Injekt.get<TranslationHostActions>(),
                automaticSelectionEnabled =
                automaticTranslationSettings.automaticSelectionEnabled(settingsSurfaceId),
                scope = lifecycleScope,
                initialCapabilities = session.readerCapabilities,
            ).also { translationController = it }
            activeTranslationController.updateCapabilities(session.readerCapabilities)
            val window = chapterCoordinator.resolveWindow(session)
            val focusedText = currentFocus as? BookDocumentTextView
            val retainFocusedText = shouldRetainProseTextFocus(
                focusedSectionKey = focusedText?.documentSectionKey,
                retainedSectionKeys = listOfNotNull(
                    window.previous?.id?.toString(),
                    window.current.id.toString(),
                    window.next?.id?.toString(),
                ).toSet(),
                resetViewer = resetViewer,
            )
            if (!retainFocusedText) {
                activeTranslationController.clearSelection()
            }
            if (settings == null) {
                settings = HtmlProseSettingsBinding(
                    provider = Injekt.get<HtmlProseSettingsProvider>(),
                    binder = Injekt.get<ViewerSettingBinder>(),
                    entryId = session.entry.id,
                    readerSettingsSurfaceId = settingsSurfaceId,
                    readerCapabilities = session.readerCapabilities,
                ).also { it.awaitInitialLayoutMode() }
            }
            val locator = retainedSession.currentLocator
                ?.takeIf(content::validate)
                ?: BookLocator(content.resourceId, progression = 0.0)
            val initialPosition = content.document.document.resolvePosition(locator)
                ?: content.document.document.positionAtProgression((locator.progression ?: 0.0).toFloat())
            openedSession = session
            latestLocator = locator
            pageLoaded = false
            val loadedChapter = HtmlProseLoadedChapter(
                key = session.chapter.id.toString(),
                owner = session.chapter,
                document = content.document,
                initialPosition = initialPosition,
                resourceLoader = content.resourceLoader,
            )
            if (uiState != null && !retainFocusedText) {
                // Release Android View focus before Compose removes outgoing lazy-list items.
                // Otherwise focus reassignment can remeasure the list while changes are applying.
                currentFocus?.clearFocus()
            }
            uiState = HtmlProseReaderUiState(
                entryTitle = session.entry.displayTitle,
                currentChapterId = session.chapter.id,
                chapters = chapterCoordinator.chapters,
                window = window,
                loadedChapters = uiState?.loadedChapters.orEmpty() + (session.chapter.id to loadedChapter),
                viewerResetKey = (uiState?.viewerResetKey ?: 0L) + if (resetViewer) 1L else 0L,
            )
            resolveChildWebView(session)
            readingStartedAt = SystemClock.elapsedRealtime()
            surfaceState = ProseReaderSurfaceState.Ready
            setMenuVisibility(false)
            chapterCoordinator.onSessionShown(locator.progression)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            translationController?.clearSelection()
            retainedSession.release()
            showError(error.message ?: getString(R.string.prose_reader_incompatible_session))
        }
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        uiState?.childWebView?.url?.let { outContent.webUri = Uri.parse(it) }
    }

    private fun resolveChildWebView(session: OpenedBookReaderSession) {
        childWebViewResolver.resolve(session)
    }

    private fun launchChildWebViewAction(
        action: EntryChildWebViewAction,
        resolution: EntryChildWebViewResolution.Available,
    ) {
        launchEntryChildWebViewAction(action, resolution, openedSession?.entry?.displayTitle)
            .onFailure {
                Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
            }
    }

    private fun launchExternalLink(url: String) {
        runCatching {
            val uri = url.toValidatedProseExternalUri()
            startActivity(Intent(Intent.ACTION_VIEW, uri))
        }.onFailure {
            Toast.makeText(this, it.message, Toast.LENGTH_LONG).show()
        }
    }

    private fun updateLocation(
        chapterId: Long,
        progression: Float,
        position: BookDocumentPosition?,
    ) {
        if (chapterId != openedSession?.chapter?.id) return
        val loaded = uiState?.loadedChapters?.get(chapterId) ?: return
        pageLoaded = true
        val locator = position
            ?.takeIf(loaded.document.document::contains)
            ?.let(loaded.document.document::locatorAt)
            ?: BookLocator(
                resourceId = loaded.document.document.resourceId,
                progression = progression.coerceIn(0f, 1f).toDouble(),
            )
        latestLocator = locator
        retainedSession.updateLocation(locator)
        chapterCoordinator.prepareNextChapterIfNeeded(locator.progression)
    }

    private fun setMenuVisibility(visible: Boolean) {
        uiState = uiState?.copy(menuVisible = visible)
        if (visible) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun persist(
        elapsed: Long,
        persistLocation: Boolean = true,
        forceCompleted: Boolean = false,
        after: (() -> Unit)? = null,
    ) {
        val session = openedSession ?: return after?.invoke() ?: Unit
        // The retained locator hands progress to the recreated Activity. Letting this Activity
        // persist its final location asynchronously could overwrite newer progress from that one.
        val locator = latestLocator.takeIf { persistLocation }
        if (locator == null && elapsed <= 0L) return after?.invoke() ?: Unit
        val completed =
            forceCompleted || (pageLoaded && locator?.progression?.let { it >= COMPLETION_THRESHOLD } == true)
        lifecycleScope.launchNonCancellable {
            locator?.let { session.saveLocation(it, completed = completed) }
            session.recordHistory(elapsed)
            lifecycle.withStarted { after?.invoke() }
        }
    }

    private suspend fun showError(message: String) {
        lifecycle.withStarted {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
            surfaceState = ProseReaderSurfaceState.Error(message)
        }
    }

    companion object {
        private const val EXTRA_ENTRY_ID = "entry_id"
        private const val EXTRA_CHAPTER_ID = "chapter_id"
        private const val EXTRA_PROCESSOR_ID = "processor_id"
        private const val EXTRA_SESSION_TOKEN = "session_token"
        private const val COMPLETION_THRESHOLD = 0.995

        fun newIntent(
            context: Context,
            request: BookReaderRequest,
            processorId: String,
            sessionToken: String,
        ): Intent = Intent(context, HtmlProseChapterReaderActivity::class.java).apply {
            putExtra(EXTRA_ENTRY_ID, request.entryId)
            putExtra(EXTRA_CHAPTER_ID, request.chapterId)
            putExtra(EXTRA_PROCESSOR_ID, processorId)
            putExtra(EXTRA_SESSION_TOKEN, sessionToken)
        }
    }
}

private sealed interface ProseReaderSurfaceState {
    data object Loading : ProseReaderSurfaceState
    data object Ready : ProseReaderSurfaceState
    data class Error(val message: String) : ProseReaderSurfaceState
}
