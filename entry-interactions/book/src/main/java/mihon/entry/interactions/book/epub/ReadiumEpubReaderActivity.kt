package mihon.entry.interactions.book.epub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.launch
import mihon.entry.interactions.book.BookReaderOpenResult
import mihon.entry.interactions.book.BookReaderRequest
import mihon.entry.interactions.book.BookReaderSessionFactory
import mihon.entry.interactions.book.OpenedBookReaderSession
import mihon.entry.interactions.book.R
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import tachiyomi.core.common.util.lang.launchNonCancellable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Processor-owned EPUB reader surface. Generic BOOK code only launches this entry point. */
internal class ReadiumEpubReaderActivity : FragmentActivity() {
    private val containerId = FrameLayout.generateViewId()
    private lateinit var root: FrameLayout
    private var openedSession: OpenedBookReaderSession? = null
    private var readerHost: ReadiumEpubReaderHost? = null
    private var navigator: EpubNavigatorFragment? = null
    private var readingStartedAt: Long? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        // Reopen from persisted BOOK progress instead of restoring a Fragment whose Publication is process-scoped.
        super.onCreate(null)
        root = FrameLayout(this).apply { id = containerId }
        setContentView(
            root,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        showLoading()

        val request = BookReaderRequest(
            entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L),
            chapterId = intent.getLongExtra(EXTRA_CHAPTER_ID, -1L),
        )
        val processorId = intent.getStringExtra(EXTRA_PROCESSOR_ID)
        if (request.entryId < 0L || request.chapterId < 0L || processorId.isNullOrBlank()) {
            showError(getString(R.string.book_reader_invalid_request))
            return
        }

        lifecycleScope.launch {
            val result = Injekt.get<BookReaderSessionFactory>().open(
                context = this@ReadiumEpubReaderActivity,
                request = request,
                processorId = processorId,
            )
            when (result) {
                is BookReaderOpenResult.Failure -> lifecycle.withStarted {
                    showError(result.failure.message)
                }
                is BookReaderOpenResult.Success -> {
                    var installed = false
                    try {
                        lifecycle.withStarted {
                            showReader(result.session)
                            installed = true
                        }
                    } finally {
                        if (!installed) result.session.close()
                    }
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

    override fun onStop() {
        val session = openedSession
        val elapsed = readingStartedAt
            ?.let { (SystemClock.elapsedRealtime() - it).coerceAtLeast(0L) }
            ?: 0L
        readingStartedAt = null
        val currentLocation = navigator?.let { readerHost?.currentLocation(it) }
        if (session != null && (currentLocation != null || elapsed > 0L)) {
            lifecycleScope.launchNonCancellable {
                currentLocation?.let { session.saveLocation(it) }
                session.recordHistory(elapsed)
            }
        }
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        openedSession?.close()
        openedSession = null
        readerHost = null
        navigator = null
    }

    private fun showReader(session: OpenedBookReaderSession) {
        val publicationSession = session.publicationSession as? ReadiumPublicationSession
            ?: run {
                session.close()
                showError(getString(R.string.book_reader_incompatible_session))
                return
            }
        title = publicationSession.publication.title ?: session.entry.displayTitle
        root.removeAllViews()
        val host = ReadiumEpubReaderHost(publicationSession)
        val fragmentFactory = host.createFragmentFactory(session.initialLocator)
        supportFragmentManager.fragmentFactory = fragmentFactory
        val fragment = fragmentFactory.instantiate(
            classLoader,
            EpubNavigatorFragment::class.java.name,
        ) as EpubNavigatorFragment
        supportFragmentManager.commitNow {
            replace(containerId, fragment)
        }
        openedSession = session
        readerHost = host
        navigator = fragment
        readingStartedAt = SystemClock.elapsedRealtime()
        host.observeLocations(fragment, lifecycleScope) { locator ->
            session.saveLocation(locator)
        }
    }

    private fun showLoading() {
        root.removeAllViews()
        root.addView(
            ProgressBar(this),
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER,
            ),
        )
    }

    private fun showError(message: String) {
        root.removeAllViews()
        val spacing = (resources.displayMetrics.density * 24).toInt()
        root.addView(
            TextView(this).apply {
                text = message
                textSize = 16f
                gravity = Gravity.CENTER
                setPadding(spacing, spacing, spacing, spacing)
            },
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
    }

    companion object {
        private const val EXTRA_ENTRY_ID = "entry_id"
        private const val EXTRA_CHAPTER_ID = "chapter_id"
        private const val EXTRA_PROCESSOR_ID = "processor_id"

        fun newIntent(
            context: Context,
            request: BookReaderRequest,
            processorId: String,
        ): Intent = Intent(context, ReadiumEpubReaderActivity::class.java).apply {
            putExtra(EXTRA_ENTRY_ID, request.entryId)
            putExtra(EXTRA_CHAPTER_ID, request.chapterId)
            putExtra(EXTRA_PROCESSOR_ID, processorId)
        }
    }
}
