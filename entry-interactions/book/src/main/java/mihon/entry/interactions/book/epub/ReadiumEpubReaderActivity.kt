@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package mihon.entry.interactions.book.epub

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.launch
import mihon.book.api.BookNavigationItem
import mihon.book.api.BookReadingDirection
import mihon.entry.interactions.EntryInteractionActivity
import mihon.entry.interactions.book.BookReaderErrorScreen
import mihon.entry.interactions.book.BookReaderLoadingScreen
import mihon.entry.interactions.book.BookReaderOpenResult
import mihon.entry.interactions.book.BookReaderRequest
import mihon.entry.interactions.book.BookReaderSessionFactory
import mihon.entry.interactions.book.OpenedBookReaderSession
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.displayName
import mihon.entry.interactions.setEntryInteractionContent
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import mihon.entry.viewer.settings.ViewerSettingBinder
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.publication.Locator
import tachiyomi.core.common.util.lang.launchNonCancellable
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Processor-owned EPUB reader surface. Generic BOOK code only launches this entry point. */
internal class ReadiumEpubReaderActivity : EntryInteractionActivity() {
    private val containerId = FrameLayout.generateViewId()
    private lateinit var readerContainer: FrameLayout
    private lateinit var composeOverlay: ComposeView
    private var openedSession: OpenedBookReaderSession? = null
    private var readerHost: ReadiumEpubReaderHost? = null
    private var navigator: EpubNavigatorFragment? = null
    private var settings: ReadiumEpubSettingsBinding? = null
    private var inputListener: InputListener? = null
    private var readingStartedAt: Long? = null
    private var surfaceState by mutableStateOf<ReaderSurfaceState>(ReaderSurfaceState.Loading)
    private var uiState by mutableStateOf(ReadiumEpubReaderUiState(bookTitle = ""))
    private var navigation by mutableStateOf<List<BookNavigationItem>>(emptyList())

    private val windowInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Reopen from persisted BOOK progress instead of restoring a Fragment whose Publication is process-scoped.
        super.onCreate(null)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        windowInsetsController.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        readerContainer = FrameLayout(this).apply {
            id = containerId
            visibility = View.INVISIBLE
        }
        composeOverlay = ComposeView(this)
        setContentView(
            FrameLayout(this).apply {
                addView(readerContainer, matchParent())
                addView(composeOverlay, matchParent())
            },
        )
        composeOverlay.setEntryInteractionContent {
            when (val state = surfaceState) {
                ReaderSurfaceState.Loading -> BookReaderLoadingScreen(
                    contentDescription = getString(R.string.book_reader_loading),
                )
                is ReaderSurfaceState.Error -> BookReaderErrorScreen(
                    title = getString(R.string.book_reader_unavailable_title),
                    message = state.message,
                    closeLabel = getString(R.string.book_reader_close),
                    onClose = ::finish,
                )
                ReaderSurfaceState.Ready -> settings?.let { readerSettings ->
                    ReadiumEpubReaderScreen(
                        state = uiState,
                        navigation = navigation,
                        settings = readerSettings,
                        onClose = ::finish,
                        onMenuVisibilityChange = ::setMenuVisibility,
                        onTocVisibilityChange = { visible ->
                            uiState = uiState.copy(tocVisible = visible)
                        },
                        onSettingsVisibilityChange = { visible ->
                            uiState = uiState.copy(settingsVisible = visible)
                        },
                        onPageIndexChange = { index ->
                            navigator?.let { fragment ->
                                if (uiState.fixedLayout) {
                                    readerHost?.goToSectionIndex(fragment, index)
                                } else {
                                    readerHost?.goToPage(fragment, index, uiState.totalPages)
                                }
                            }
                        },
                        onPreviousSection = { goToAdjacentSection(-1) },
                        onNextSection = { goToAdjacentSection(1) },
                        onNavigationItemClick = ::goToNavigationItem,
                    )
                }
            }
        }

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
                    showError(
                        getString(
                            R.string.book_reader_unavailable_message,
                            result.failure.reason.displayName(),
                            result.failure.message,
                        ),
                    )
                }
                is BookReaderOpenResult.Success -> {
                    var installed = false
                    try {
                        val readerSettings = ReadiumEpubSettingsBinding(
                            provider = Injekt.get<ReadiumEpubSettingsProvider>(),
                            binder = Injekt.get<ViewerSettingBinder>(),
                            entryId = result.session.entry.id,
                        )
                        val initialPreferences = readerSettings.initialPreferences()
                        lifecycle.withStarted {
                            showReader(result.session, readerSettings, initialPreferences)
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

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && surfaceState == ReaderSurfaceState.Ready) {
            setMenuVisibility(uiState.menuVisible)
        }
    }

    override fun onDestroy() {
        inputListener?.let { listener -> navigator?.removeInputListener(listener) }
        inputListener = null
        super.onDestroy()
        openedSession?.close()
        openedSession = null
        readerHost = null
        navigator = null
        settings = null
    }

    private fun showReader(
        session: OpenedBookReaderSession,
        readerSettings: ReadiumEpubSettingsBinding,
        initialPreferences: EpubPreferences,
    ) {
        val publicationSession = session.publicationSession as? ReadiumPublicationSession
            ?: run {
                session.close()
                showError(getString(R.string.book_reader_incompatible_session))
                return
            }
        val host = ReadiumEpubReaderHost(publicationSession)
        val paginationListener = object : EpubNavigatorFragment.PaginationListener {
            override fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) {
                updateLocation(host, locator, pageIndex + 1, totalPages)
            }
        }
        val fragmentFactory = host.createFragmentFactory(
            initialLocator = session.initialLocator,
            initialPreferences = initialPreferences,
            paginationListener = paginationListener,
        )
        supportFragmentManager.fragmentFactory = fragmentFactory
        val fragment = fragmentFactory.instantiate(
            classLoader,
            EpubNavigatorFragment::class.java.name,
        ) as EpubNavigatorFragment
        supportFragmentManager.commitNow {
            replace(containerId, fragment)
        }

        val publication = publicationSession.publication
        title = publication.title ?: session.entry.displayTitle
        openedSession = session
        readerHost = host
        navigator = fragment
        settings = readerSettings
        navigation = publication.navigation.ifEmpty {
            publication.readingOrder.map { resource ->
                BookNavigationItem(
                    title = resource.title,
                    target = mihon.book.api.BookLocator(resourceId = resource.id),
                )
            }
        }
        uiState = ReadiumEpubReaderUiState(
            bookTitle = publication.title ?: session.entry.displayTitle,
            sectionCount = host.sectionCount,
            readingDirection = publication.readingDirection,
            fixedLayout = host.isFixedLayout,
        )
        inputListener = createInputListener(publication.readingDirection).also(fragment::addInputListener)
        readingStartedAt = SystemClock.elapsedRealtime()
        host.observeLocations(fragment, lifecycleScope) { locator ->
            session.saveLocation(locator)
            uiState = uiState.copy(
                currentLocator = locator,
                sectionTitle = fragment.currentLocator.value.title,
                currentSectionIndex = host.sectionIndex(fragment.currentLocator.value),
                currentPage = if (host.isFixedLayout) {
                    host.sectionIndex(fragment.currentLocator.value) + 1
                } else {
                    uiState.currentPage
                },
                totalPages = if (host.isFixedLayout) host.sectionCount else uiState.totalPages,
            )
        }
        host.observeSettings(fragment, readerSettings, lifecycleScope)
        readerContainer.visibility = View.VISIBLE
        surfaceState = ReaderSurfaceState.Ready
        setMenuVisibility(false)
    }

    private fun updateLocation(
        host: ReadiumEpubReaderHost,
        locator: Locator,
        currentPage: Int,
        totalPages: Int,
    ) {
        uiState = uiState.copy(
            sectionTitle = locator.title,
            currentLocator = ReadiumLocatorAdapter.adapt(locator),
            currentPage = currentPage.coerceAtLeast(1),
            totalPages = totalPages.coerceAtLeast(1),
            currentSectionIndex = host.sectionIndex(locator),
        )
    }

    private fun createInputListener(readingDirection: BookReadingDirection?): InputListener {
        return object : InputListener {
            override fun onTap(event: TapEvent): Boolean {
                val fragment = navigator ?: return false
                val readerSettings = settings ?: return false
                val paginated = readerSettings.layoutMode.state.value.effectiveValue ==
                    ReadiumEpubSettingsProvider.LAYOUT_PAGINATED
                val tapNavigation = readerSettings.tapNavigation.resolveProfile().effectiveValue
                val width = fragment.publicationView.width.takeIf { it > 0 } ?: return false
                val x = event.point.x / width.toFloat()
                if (!paginated || !tapNavigation || x in TAP_PREVIOUS_END..TAP_NEXT_START) {
                    setMenuVisibility(!uiState.menuVisible)
                    return true
                }

                val forward = when (readingDirection) {
                    BookReadingDirection.RIGHT_TO_LEFT -> x < TAP_PREVIOUS_END
                    else -> x > TAP_NEXT_START
                }
                if (forward) {
                    readerHost?.goForward(fragment)
                } else {
                    readerHost?.goBackward(fragment)
                }
                setMenuVisibility(false)
                return true
            }
        }
    }

    private fun goToAdjacentSection(direction: Int) {
        val fragment = navigator ?: return
        readerHost?.goToAdjacentSection(fragment, direction)
    }

    private fun goToNavigationItem(item: BookNavigationItem) {
        val fragment = navigator ?: return
        readerHost?.goToNavigationItem(fragment, item)
    }

    private fun setMenuVisibility(visible: Boolean) {
        uiState = uiState.copy(menuVisible = visible)
        if (visible) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        }
    }

    private fun showError(message: String) {
        readerContainer.visibility = View.INVISIBLE
        windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        surfaceState = ReaderSurfaceState.Error(message)
    }

    private fun matchParent() = FrameLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT,
        ViewGroup.LayoutParams.MATCH_PARENT,
    )

    private sealed interface ReaderSurfaceState {
        data object Loading : ReaderSurfaceState
        data object Ready : ReaderSurfaceState
        data class Error(val message: String) : ReaderSurfaceState
    }

    companion object {
        private const val EXTRA_ENTRY_ID = "entry_id"
        private const val EXTRA_CHAPTER_ID = "chapter_id"
        private const val EXTRA_PROCESSOR_ID = "processor_id"
        private const val TAP_PREVIOUS_END = 0.33f
        private const val TAP_NEXT_START = 0.67f

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
