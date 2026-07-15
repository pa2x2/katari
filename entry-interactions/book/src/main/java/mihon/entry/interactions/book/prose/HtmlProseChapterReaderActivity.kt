package mihon.entry.interactions.book.prose

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
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
import mihon.entry.interactions.viewer.EntryChildWindow
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.domain.entry.model.EntryChapter
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import kotlin.math.roundToInt

/** Processor-owned reader surface for one normalized HTML prose chapter. */
internal class HtmlProseChapterReaderActivity : EntryInteractionActivity() {
    private val retainedSession by viewModels<BookReaderSessionViewModel>()
    private var surfaceState by mutableStateOf<ProseReaderSurfaceState>(ProseReaderSurfaceState.Loading)
    private var openedSession: OpenedBookReaderSession? = null
    private var proseSession: HtmlProseChapterSession? = null
    private var webView: WebView? = null
    private var latestLocator: BookLocator? = null
    private var navigation: EntryChildWindow<EntryChapter>? = null
    private var readingStartedAt: Long? = null
    private var pageLoaded = false
    private var stopPersistenceSuppressed = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(null)
        setEntryInteractionContent {
            when (val state = surfaceState) {
                ProseReaderSurfaceState.Loading -> BookReaderLoadingScreen(
                    contentDescription = stringResource(R.string.book_reader_loading),
                )
                is ProseReaderSurfaceState.Error -> BookReaderErrorScreen(
                    title = stringResource(R.string.book_reader_unavailable_title),
                    message = state.message,
                    closeLabel = stringResource(R.string.book_reader_close),
                    onClose = ::finish,
                )
                is ProseReaderSurfaceState.Ready -> key(state.resourceId) {
                    HtmlProseChapterReaderScreen(
                        state = state,
                        onWebView = { webView = it },
                        onLocation = ::updateLocation,
                        onClose = ::finish,
                        onPrevious = navigation?.previous?.let { chapter -> { openAdjacent(chapter, false) } },
                        onNext = navigation?.next?.let { chapter -> { openAdjacent(chapter, true) } },
                    )
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
        if (!stopPersistenceSuppressed) {
            persist(elapsed)
        }
        super.onStop()
    }

    override fun onDestroy() {
        webView?.apply {
            stopLoading()
            loadUrl("about:blank")
            clearHistory()
            removeAllViews()
            destroy()
        }
        webView = null
        openedSession = null
        proseSession = null
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
            is BookReaderOpenResult.Success -> {
                if (retainedSession.session == null) retainedSession.attach(result.session)
                val content = result.session.publicationSession as? HtmlProseChapterSession
                if (content == null) {
                    retainedSession.release()
                    showError(getString(R.string.prose_reader_incompatible_session))
                    return
                }
                try {
                    navigation = Injekt.get<BookChapterNavigationResolver>()
                        .resolve(result.session.entry, result.session.chapter)
                    val locator = retainedSession.currentLocator
                        ?.takeIf(content::validate)
                        ?: BookLocator(content.resourceId, progression = 0.0)
                    openedSession = result.session
                    proseSession = content
                    latestLocator = locator
                    surfaceState = ProseReaderSurfaceState.Ready(
                        title = result.session.chapter.name,
                        resourceId = content.resourceId,
                        bodyHtml = content.bodyHtml,
                        initialProgression = locator.progression ?: 0.0,
                    )
                    readingStartedAt = SystemClock.elapsedRealtime()
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Exception) {
                    retainedSession.release()
                    showError(error.message ?: getString(R.string.prose_reader_incompatible_session))
                }
            }
        }
    }

    private fun updateLocation(progression: Double) {
        val resourceId = proseSession?.resourceId ?: return
        pageLoaded = true
        val locator = BookLocator(resourceId = resourceId, progression = progression.coerceIn(0.0, 1.0))
        latestLocator = locator
        retainedSession.updateLocation(locator)
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
    data class Error(val message: String) : ProseReaderSurfaceState
    data class Ready(
        val title: String,
        val resourceId: String,
        val bodyHtml: String,
        val initialProgression: Double,
    ) : ProseReaderSurfaceState
}

@Composable
private fun HtmlProseChapterReaderScreen(
    state: ProseReaderSurfaceState.Ready,
    onWebView: (WebView) -> Unit,
    onLocation: (Double) -> Unit,
    onClose: () -> Unit,
    onPrevious: (() -> Unit)?,
    onNext: (() -> Unit)?,
) {
    val colors = MaterialTheme.colorScheme
    val html = buildReaderDocument(
        bodyHtml = state.bodyHtml,
        background = colors.background.toArgb(),
        foreground = colors.onBackground.toArgb(),
        link = colors.primary.toArgb(),
    )
    Surface(modifier = Modifier.fillMaxSize(), color = colors.background) {
        Column {
            TopAppBar(
                title = { Text(state.title, maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = stringResource(R.string.book_reader_close))
                    }
                },
                actions = {
                    IconButton(onClick = { onPrevious?.invoke() }, enabled = onPrevious != null) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.prose_reader_previous_chapter),
                        )
                    }
                    IconButton(onClick = { onNext?.invoke() }, enabled = onNext != null) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = stringResource(R.string.prose_reader_next_chapter),
                        )
                    }
                },
            )
            Row(modifier = Modifier.weight(1f)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        createSecureWebView(
                            context = context,
                            html = html,
                            initialProgression = state.initialProgression,
                            onLocation = onLocation,
                        ).also(onWebView)
                    },
                )
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
internal fun createSecureWebView(
    context: Context,
    html: String,
    initialProgression: Double,
    onLocation: (Double) -> Unit,
): WebView = WebView(context).apply {
    setBackgroundColor(Color.TRANSPARENT)
    settings.apply {
        javaScriptEnabled = false
        domStorageEnabled = false
        databaseEnabled = false
        allowFileAccess = false
        allowContentAccess = false
        blockNetworkLoads = true
        loadsImagesAutomatically = false
        setSupportZoom(false)
        builtInZoomControls = false
        displayZoomControls = false
    }
    webViewClient = object : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
            !request.url.isReaderAnchor()

        @Deprecated("Deprecated in Android")
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean =
            !Uri.parse(url).isReaderAnchor()

        override fun onPageFinished(view: WebView, url: String) {
            view.post {
                val range = view.scrollRange()
                view.scrollTo(0, (range * initialProgression.coerceIn(0.0, 1.0)).roundToInt())
                onLocation(view.progression())
            }
        }
    }
    setOnScrollChangeListener { view: View, _, _, _, _ ->
        onLocation((view as WebView).progression())
    }
    loadDataWithBaseURL(READER_BASE_URL, html, HTML_MEDIA_TYPE, "utf-8", null)
}

private fun WebView.progression(): Double {
    val range = scrollRange()
    return if (range <= 0) 1.0 else scrollY.toDouble().div(range).coerceIn(0.0, 1.0)
}

private fun WebView.scrollRange(): Int =
    ((contentHeight * scale).roundToInt() - height).coerceAtLeast(0)

private fun Uri.isReaderAnchor(): Boolean =
    scheme == "https" && host == READER_HOST && fragment != null

private fun buildReaderDocument(bodyHtml: String, background: Int, foreground: Int, link: Int): String = """
    <!doctype html>
    <html>
    <head>
      <meta name="viewport" content="width=device-width, initial-scale=1">
      <style>
        :root { color-scheme: light dark; }
        html { background: ${background.cssColor()}; color: ${foreground.cssColor()}; }
        body { max-width: 46rem; margin: 0 auto; padding: 1.25rem 1.25rem 4rem; font: 1.08rem/1.72 sans-serif; }
        h1, h2, h3, h4, h5, h6 { line-height: 1.25; }
        a { color: ${link.cssColor()}; }
        blockquote { margin-inline: 0; padding-inline-start: 1rem; border-inline-start: 0.2rem solid currentColor; opacity: 0.85; }
        pre, code { white-space: pre-wrap; overflow-wrap: anywhere; }
        table { display: block; max-width: 100%; overflow-x: auto; border-collapse: collapse; }
        th, td { padding: 0.35rem 0.5rem; border: 1px solid currentColor; }
      </style>
    </head>
    <body>$bodyHtml</body>
    </html>
""".trimIndent()

private fun Int.cssColor(): String = "#%06X".format(this and 0xFFFFFF)

private const val READER_HOST = "katari.invalid"
private const val READER_BASE_URL = "https://$READER_HOST/"
