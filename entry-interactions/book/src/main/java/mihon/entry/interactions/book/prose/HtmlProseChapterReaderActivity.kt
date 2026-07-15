package mihon.entry.interactions.book.prose

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
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
import mihon.book.api.BookLocator
import mihon.entry.interactions.EntryInteractionActivity
import mihon.entry.interactions.book.BookChapterNavigationResolver
import mihon.entry.interactions.book.BookReaderErrorScreen
import mihon.entry.interactions.book.BookReaderHostActivity
import mihon.entry.interactions.book.BookReaderLoadingScreen
import mihon.entry.interactions.book.BookReaderOpenResult
import mihon.entry.interactions.book.BookReaderRequest
import mihon.entry.interactions.book.BookReaderSessionFactory
import mihon.entry.interactions.book.BookReaderSessionRegistry
import mihon.entry.interactions.book.BookReaderSessionViewModel
import mihon.entry.interactions.book.OpenedBookReaderSession
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.displayName
import mihon.entry.interactions.setEntryInteractionContent
import mihon.entry.interactions.settings.HtmlProseSettingsProvider
import mihon.entry.interactions.viewer.EntryChildWindow
import mihon.entry.viewer.settings.ViewerSettingBinder
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.EntryChapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Processor-owned reader surface for independently loaded HTML prose chapters. */
internal class HtmlProseChapterReaderActivity : EntryInteractionActivity() {
    private val retainedSession by viewModels<BookReaderSessionViewModel>()
    private var surfaceState by mutableStateOf<ProseReaderSurfaceState>(ProseReaderSurfaceState.Loading)
    private var uiState by mutableStateOf<HtmlProseReaderUiState?>(null)
    private var settings: HtmlProseSettingsBinding? = null
    private var openedSession: OpenedBookReaderSession? = null
    private var proseSession: HtmlProseChapterSession? = null
    private var webView: ProseWebView? = null
    private var latestLocator: BookLocator? = null
    private var navigation: EntryChildWindow<EntryChapter>? = null
    private var readingStartedAt: Long? = null
    private var pageLoaded = false
    private var stopPersistenceSuppressed = false

    private val windowInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        windowInsetsController.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
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
                            onWebView = { webView = it },
                            onLocation = ::updateLocation,
                            onTap = ::onReaderTap,
                            onClose = ::finish,
                            onPreviousChapter = navigation?.previous?.let { chapter ->
                                { openAdjacent(chapter, completeCurrent = false) }
                            },
                            onNextChapter = navigation?.next?.let { chapter ->
                                { openAdjacent(chapter, completeCurrent = true) }
                            },
                            onSettingsVisibilityChange = { visible ->
                                uiState = uiState?.copy(settingsVisible = visible)
                            },
                        )
                    }
                }
            }
        }
        lifecycleScope.launch { open() }
    }

    override fun onStart() {
        super.onStart()
        if (openedSession != null && readingStartedAt == null) {
            readingStartedAt = SystemClock.elapsedRealtime()
        }
    }

    override fun onStop() {
        val elapsed = readingStartedAt
            ?.let { (SystemClock.elapsedRealtime() - it).coerceAtLeast(0L) }
            ?: 0L
        readingStartedAt = null
        if (!stopPersistenceSuppressed) persist(elapsed)
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && surfaceState == ProseReaderSurfaceState.Ready) {
            setMenuVisibility(uiState?.menuVisible == true)
        }
    }

    override fun onDestroy() {
        webView = null
        openedSession = null
        proseSession = null
        settings = null
        super.onDestroy()
    }

    private suspend fun open() {
        val retained = retainedSession.session
        val request = retained?.let { BookReaderRequest(it.entry.id, it.chapter.id) } ?: BookReaderRequest(
            entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L),
            chapterId = intent.getLongExtra(EXTRA_CHAPTER_ID, -1L),
        )
        val processorId = intent.getStringExtra(EXTRA_PROCESSOR_ID)
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
            is BookReaderOpenResult.Success -> showSession(result.session)
        }
    }

    private suspend fun showSession(session: OpenedBookReaderSession) {
        if (retainedSession.session == null) retainedSession.attach(session)
        val content = session.publicationSession as? HtmlProseChapterSession
        if (content == null) {
            retainedSession.release()
            showError(getString(R.string.prose_reader_incompatible_session))
            return
        }
        try {
            navigation = Injekt.get<BookChapterNavigationResolver>().resolve(session.entry, session.chapter)
            settings = HtmlProseSettingsBinding(
                provider = Injekt.get<HtmlProseSettingsProvider>(),
                binder = Injekt.get<ViewerSettingBinder>(),
                entryId = session.entry.id,
            )
            val locator = retainedSession.currentLocator
                ?.takeIf(content::validate)
                ?: BookLocator(content.resourceId, progression = 0.0)
            openedSession = session
            proseSession = content
            latestLocator = locator
            uiState = HtmlProseReaderUiState(
                entryTitle = session.entry.displayTitle,
                chapterTitle = session.chapter.name,
                resourceId = content.resourceId,
                bodyHtml = content.bodyHtml,
                progression = (locator.progression ?: 0.0).toFloat(),
            )
            readingStartedAt = SystemClock.elapsedRealtime()
            surfaceState = ProseReaderSurfaceState.Ready
            setMenuVisibility(false)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            retainedSession.release()
            showError(error.message ?: getString(R.string.prose_reader_incompatible_session))
        }
    }

    private fun onReaderTap(horizontalFraction: Float) {
        val readerSettings = settings ?: return
        val paginated = readerSettings.layoutMode.state.value.effectiveValue ==
            HtmlProseSettingsProvider.LAYOUT_PAGINATED
        val tapNavigation = readerSettings.tapNavigation.resolveProfile().effectiveValue
        if (!paginated || !tapNavigation || horizontalFraction in TAP_PREVIOUS_END..TAP_NEXT_START) {
            setMenuVisibility(uiState?.menuVisible != true)
            return
        }
        val moved = webView?.movePage(if (horizontalFraction > TAP_NEXT_START) 1 else -1) == true
        if (moved) setMenuVisibility(false)
    }

    private fun updateLocation(progression: Float, currentPage: Int, totalPages: Int) {
        val resourceId = proseSession?.resourceId ?: return
        pageLoaded = true
        val safeProgression = progression.coerceIn(0f, 1f)
        val locator = BookLocator(resourceId = resourceId, progression = safeProgression.toDouble())
        latestLocator = locator
        retainedSession.updateLocation(locator)
        uiState = uiState?.copy(
            progression = safeProgression,
            currentPage = currentPage,
            totalPages = totalPages,
        )
    }

    private fun setMenuVisibility(visible: Boolean) {
        uiState = uiState?.copy(menuVisible = visible)
        if (visible) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun persist(elapsed: Long, forceCompleted: Boolean = false, after: (() -> Unit)? = null) {
        val session = openedSession ?: return after?.invoke() ?: Unit
        val locator = latestLocator
        if (locator == null && elapsed <= 0L) return after?.invoke() ?: Unit
        val completed =
            forceCompleted || (pageLoaded && locator?.progression?.let { it >= COMPLETION_THRESHOLD } == true)
        lifecycleScope.launchNonCancellable {
            locator?.let { session.saveLocation(it, completed = completed) }
            session.recordHistory(elapsed)
            lifecycle.withStarted { after?.invoke() }
        }
    }

    private fun openAdjacent(chapter: EntryChapter, completeCurrent: Boolean) {
        if (stopPersistenceSuppressed) return
        stopPersistenceSuppressed = true
        val elapsed = readingStartedAt
            ?.let { (SystemClock.elapsedRealtime() - it).coerceAtLeast(0L) }
            ?: 0L
        readingStartedAt = null
        persist(elapsed, forceCompleted = completeCurrent) {
            val session = openedSession ?: return@persist
            startActivity(BookReaderHostActivity.newIntent(this, session.entry, chapter))
            finish()
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
        private const val TAP_PREVIOUS_END = 0.33f
        private const val TAP_NEXT_START = 0.66f

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
