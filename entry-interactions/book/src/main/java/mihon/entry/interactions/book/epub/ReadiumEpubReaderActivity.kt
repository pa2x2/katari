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
import kotlinx.coroutines.Job
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
    private var navigation by mutableStateOf<List<ReadiumNavigationRow>>(emptyList())
    private val resolvedNavigationPositions = mutableMapOf<String, ReadiumNavigationPosition>()
    private var navigationResolutionJob: Job? = null
    private var navigationResolutionKey: String? = null
    private var resourceCurrentPage = 1
    private var resourceTotalPages = 1
    private var sectionStartPageIndex = 0
    private var sectionStartProgression = 0.0
    private var sectionEndProgression = 1.0
    private var pendingNavigationIndex: Int? = null

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
                        onTocVisibilityChange = { visible ->
                            uiState = uiState.copy(tocVisible = visible)
                        },
                        onSettingsVisibilityChange = { visible ->
                            uiState = uiState.copy(settingsVisible = visible)
                        },
                        onPageIndexPreview = { index ->
                            uiState = uiState.copy(currentPage = index + 1)
                        },
                        onPageIndexChange = ::goToPageInSection,
                        onProgressPreview = { progress ->
                            uiState = uiState.copy(sectionProgress = progress)
                        },
                        onProgressChange = ::goToProgressInSection,
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
        navigationResolutionJob?.cancel()
        navigationResolutionJob = null
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
                resourceCurrentPage = pageIndex + 1
                resourceTotalPages = totalPages.coerceAtLeast(1)
                updateLocation(locator)
            }

            override fun onPageLoaded() {
                navigationResolutionKey = null
                resolveCurrentNavigation()
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
        }.flattenNavigation()
        uiState = ReadiumEpubReaderUiState(
            bookTitle = publication.title ?: session.entry.displayTitle,
            sectionCount = navigation.size,
            readingDirection = publication.readingDirection,
            fixedLayout = host.isFixedLayout,
        )
        inputListener = createInputListener(publication.readingDirection).also(fragment::addInputListener)
        readingStartedAt = SystemClock.elapsedRealtime()
        host.observeLocations(fragment, lifecycleScope) { locator ->
            session.saveLocation(locator)
            uiState = uiState.copy(
                currentLocator = locator,
            )
            recalculateSectionMetrics()
            resolveCurrentNavigation()
        }
        host.observeSettings(fragment, readerSettings, lifecycleScope)
        readerContainer.visibility = View.VISIBLE
        surfaceState = ReaderSurfaceState.Ready
        setMenuVisibility(false)
    }

    private fun updateLocation(locator: Locator) {
        uiState = uiState.copy(
            currentLocator = ReadiumLocatorAdapter.adapt(locator),
        )
        recalculateSectionMetrics()
        resolveCurrentNavigation()
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
        val currentIndex = uiState.currentSectionIndex
        if (currentIndex !in navigation.indices) return
        val currentTarget = navigation[currentIndex].item.target.navigationKey()
        val target = generateSequence(currentIndex + direction) { it + direction }
            .takeWhile { it in navigation.indices }
            .firstOrNull { navigation[it].item.target.navigationKey() != currentTarget }
            ?: return
        goToNavigationItem(navigation[target].item)
    }

    private fun goToNavigationItem(item: BookNavigationItem) {
        val fragment = navigator ?: return
        val index = navigation.indexOfFirst { it.item == item }
        if (index >= 0) {
            pendingNavigationIndex = index
            uiState = uiState.copy(
                currentSectionIndex = index,
                sectionTitle = item.title,
                sectionProgress = 0f,
                currentPage = 1,
            )
        }
        if (readerHost?.goToNavigationItem(fragment, item) != true) {
            pendingNavigationIndex = null
            recalculateSectionMetrics()
        }
    }

    private fun goToPageInSection(pageIndex: Int) {
        val fragment = navigator ?: return
        val target = (sectionStartPageIndex + pageIndex).coerceIn(0, resourceTotalPages - 1)
        uiState = uiState.copy(currentPage = pageIndex + 1)
        readerHost?.goToPage(fragment, target, resourceTotalPages)
    }

    private fun goToProgressInSection(progress: Float) {
        val fragment = navigator ?: return
        val safeProgress = progress.coerceIn(0f, 1f)
        val target = sectionStartProgression +
            (sectionEndProgression - sectionStartProgression) * safeProgress
        uiState = uiState.copy(sectionProgress = safeProgress)
        readerHost?.goToProgression(fragment, target)
    }

    private fun resolveCurrentNavigation() {
        val fragment = navigator ?: return
        val host = readerHost ?: return
        val locator = uiState.currentLocator ?: return
        val paginated = isPaginated()
        val key = "${locator.resourceId}|$paginated|$resourceTotalPages"
        if (navigationResolutionKey == key) return
        navigationResolutionKey = key
        navigationResolutionJob?.cancel()
        navigationResolutionJob = lifecycleScope.launch {
            val positions = host.resolveNavigationProgressions(
                navigator = fragment,
                navigation = navigation,
                resourceId = locator.resourceId,
                paginated = paginated,
            )
            if (uiState.currentLocator?.resourceId != locator.resourceId || isPaginated() != paginated) return@launch
            resolvedNavigationPositions.putAll(positions)
            recalculateSectionMetrics()
        }
    }

    private fun recalculateSectionMetrics() {
        val locator = uiState.currentLocator ?: return
        val preferredIndex = pendingNavigationIndex ?: uiState.currentSectionIndex
        val paginatedMetrics = if (isPaginated()) {
            resolvePaginatedSectionMetrics(
                navigation = navigation,
                locator = locator,
                resolvedPositions = resolvedNavigationPositions,
                currentPageIndex = resourceCurrentPage - 1,
                totalPages = resourceTotalPages,
                preferredIndex = preferredIndex,
            )
        } else {
            null
        }
        val scrollingMetrics = if (paginatedMetrics == null) {
            resolveSectionMetrics(
                navigation = navigation,
                locator = locator,
                resolvedPositions = resolvedNavigationPositions,
                preferredIndex = preferredIndex,
            )
        } else {
            null
        }
        val fallbackIndex = preferredIndex
            .takeIf { it in navigation.indices && navigation[it].item.target.resourceId == locator.resourceId }
            ?: navigation.indexOfFirst { it.item.target.resourceId == locator.resourceId }
        val sectionIndex = paginatedMetrics?.index ?: scrollingMetrics?.index ?: fallbackIndex
        val sectionStart = paginatedMetrics?.startProgression ?: scrollingMetrics?.startProgression ?: 0.0
        val sectionEnd = paginatedMetrics?.endProgression ?: scrollingMetrics?.endProgression ?: 1.0
        sectionStartProgression = sectionStart
        sectionEndProgression = sectionEnd

        val currentProgression = locator.progression ?: sectionStart
        val sectionProgress = if (sectionEnd - sectionStart > 0.0001) {
            ((currentProgression - sectionStart) / (sectionEnd - sectionStart)).coerceIn(0.0, 1.0)
        } else {
            0.0
        }
        val startPageIndex = paginatedMetrics?.startPageIndex
            ?: (sectionStart * resourceTotalPages).toInt().coerceIn(0, resourceTotalPages - 1)
        val endPageIndex = paginatedMetrics?.endPageIndex
            ?: (sectionEnd * resourceTotalPages).toInt().coerceIn(startPageIndex + 1, resourceTotalPages)
        sectionStartPageIndex = startPageIndex
        val totalPages = (endPageIndex - startPageIndex).coerceAtLeast(1)
        val currentPage = (resourceCurrentPage - startPageIndex).coerceIn(1, totalPages)

        if (pendingNavigationIndex == sectionIndex) pendingNavigationIndex = null
        uiState = uiState.copy(
            sectionTitle = navigation.getOrNull(sectionIndex)?.item?.title,
            currentSectionIndex = sectionIndex,
            currentPage = currentPage,
            totalPages = totalPages,
            sectionProgress = sectionProgress.toFloat(),
        )
    }

    private fun isPaginated(): Boolean = uiState.fixedLayout ||
        settings?.layoutMode?.state?.value?.effectiveValue == ReadiumEpubSettingsProvider.LAYOUT_PAGINATED

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
