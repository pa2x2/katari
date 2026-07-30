@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)

package mihon.entry.interactions.book.epub

import android.app.assist.AssistContent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.withStarted
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import logcat.LogPriority
import mihon.book.api.BookLocator
import mihon.book.api.BookNavigationItem
import mihon.entry.interactions.EntryChildWebViewAction
import mihon.entry.interactions.EntryChildWebViewResolution
import mihon.entry.interactions.EntryInteractionActivity
import mihon.entry.interactions.EntryWebViewFeature
import mihon.entry.interactions.book.BookAutomaticTranslationSettingsProvider
import mihon.entry.interactions.book.BookChildWebViewResolver
import mihon.entry.interactions.book.BookReaderErrorScreen
import mihon.entry.interactions.book.BookReaderLoadingScreen
import mihon.entry.interactions.book.BookReaderOpenResult
import mihon.entry.interactions.book.BookReaderRequest
import mihon.entry.interactions.book.BookReaderSessionFactory
import mihon.entry.interactions.book.BookReaderSessionRegistry
import mihon.entry.interactions.book.BookReaderSessionViewModel
import mihon.entry.interactions.book.BookSelectionTranslationController
import mihon.entry.interactions.book.OpenedBookReaderSession
import mihon.entry.interactions.book.R
import mihon.entry.interactions.book.displayName
import mihon.entry.interactions.launchEntryChildWebViewAction
import mihon.entry.interactions.setEntryInteractionContent
import mihon.entry.interactions.settings.ReadiumEpubSettingsProvider
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.translation.api.TranslationFeature
import mihon.translation.api.TranslationHostActions
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.EpubPreferences
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.shared.publication.Locator
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.system.logcat
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

/** Processor-owned EPUB reader surface. Generic BOOK code only launches this entry point. */
internal class ReadiumEpubReaderActivity : EntryInteractionActivity() {
    internal val retainedSession by viewModels<BookReaderSessionViewModel>()
    private val containerId = FrameLayout.generateViewId()
    private lateinit var readerContainer: FrameLayout
    private lateinit var composeOverlay: ComposeView
    private var openedSession: OpenedBookReaderSession? = null
    internal var readerHost: ReadiumEpubReaderHost? = null
    internal var navigator: EpubNavigatorFragment? = null
    internal var settings: ReadiumEpubSettingsBinding? = null
    private var inputListener: InputListener? = null
    private var readingStartedAt: Long? = null
    private var surfaceState by mutableStateOf<ReaderSurfaceState>(ReaderSurfaceState.Loading)
    internal var uiState by mutableStateOf(ReadiumEpubReaderUiState(bookTitle = ""))
    internal var navigation by mutableStateOf<List<ReadiumNavigationRow>>(emptyList())
    private lateinit var navigationController: ReadiumEpubNavigationController
    private lateinit var childWebViewResolver: BookChildWebViewResolver
    internal var translationController: BookSelectionTranslationController? = null
    private var selectionCoordinator: ReadiumEpubSelectionCoordinator? = null
    private var readerRootPositionInWindow: Offset = Offset.Zero

    private val windowInsetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }
    override fun onCreate(savedInstanceState: Bundle?) {
        // Reopen from persisted BOOK progress instead of restoring a Fragment whose Publication is process-scoped.
        super.onCreate(null)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        windowInsetsController.systemBarsBehavior =
            androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        navigationController = ReadiumEpubNavigationController(this)
        childWebViewResolver = BookChildWebViewResolver(
            scope = lifecycleScope,
            feature = Injekt.get<EntryWebViewFeature>(),
            currentChapterId = { openedSession?.chapter?.id },
            onResolution = { resolution ->
                uiState = uiState.copy(childWebView = resolution)
            },
            onFailure = { error ->
                logcat(LogPriority.ERROR, error) { "Failed to resolve BOOK child WebView URL" }
            },
        )

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
                        nativeContentView = readerContainer,
                        onClose = ::finish,
                        onTocVisibilityChange = { visible ->
                            uiState = uiState.copy(tocVisible = visible)
                        },
                        onSettingsVisibilityChange = { visible ->
                            uiState = uiState.copy(settingsVisible = visible)
                        },
                        onPageIndexPreview = navigationController::previewPageInSection,
                        onPageIndexChange = navigationController::finishPageInSection,
                        onPreviousSection = { navigationController.goToAdjacentSection(-1) },
                        onNextSection = { navigationController.goToAdjacentSection(1) },
                        onNavigationItemClick = navigationController::goToNavigationItem,
                        onChildWebViewAction = ::launchChildWebViewAction,
                        translationController = translationController,
                        onReaderRootPositionInWindow = { readerRootPositionInWindow = it },
                    )
                }
            }
        }

        val request = BookReaderRequest(
            entryId = intent.getLongExtra(EXTRA_ENTRY_ID, -1L),
            chapterId = intent.getLongExtra(EXTRA_CHAPTER_ID, -1L),
        )
        val processorId = intent.getStringExtra(EXTRA_PROCESSOR_ID)
        val sessionToken = intent.getStringExtra(EXTRA_SESSION_TOKEN)
        if (request.entryId < 0L || request.chapterId < 0L || processorId.isNullOrBlank()) {
            showError(getString(R.string.book_reader_invalid_request))
            return
        }

        lifecycleScope.launch {
            val retained = retainedSession.session
            val handedOff = if (retained == null && !sessionToken.isNullOrBlank()) {
                Injekt.get<BookReaderSessionRegistry>().claim(sessionToken, request)
            } else {
                null
            }
            val result = when (val session = retained ?: handedOff) {
                null -> Injekt.get<BookReaderSessionFactory>().open(
                    context = this@ReadiumEpubReaderActivity,
                    request = request,
                    processorId = processorId,
                )
                else -> BookReaderOpenResult.Success(session)
            }
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
                    if (retainedSession.session == null) retainedSession.attach(result.session)
                    try {
                        val settingsSurfaceId = requireNotNull(result.session.readerSettingsSurfaceId) {
                            "The EPUB reader session has no viewer settings surface"
                        }
                        val readerSettings = ReadiumEpubSettingsBinding(
                            provider = Injekt.get<ReadiumEpubSettingsProvider>(),
                            binder = Injekt.get<ViewerSettingBinder>(),
                            readerSettingsSurfaceId = settingsSurfaceId,
                            readerCapabilities = result.session.readerCapabilities,
                        )
                        val initialPreferences = readerSettings.initialPreferences()
                        lifecycle.withStarted {
                            if (!showReader(result.session, readerSettings, initialPreferences)) {
                                retainedSession.release()
                            }
                        }
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Exception) {
                        retainedSession.release()
                        lifecycle.withStarted {
                            showError(error.message ?: getString(R.string.book_reader_incompatible_session))
                        }
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

    override fun onResume() {
        super.onResume()
        translationController?.onResume()
    }

    override fun onStop() {
        val session = openedSession
        val elapsed = readingStartedAt
            ?.let { (SystemClock.elapsedRealtime() - it).coerceAtLeast(0L) }
            ?: 0L
        readingStartedAt = null
        val currentLocation = navigator?.let { readerHost?.currentLocation(it) }
        currentLocation?.let(retainedSession::updateLocation)
        if (session != null && (currentLocation != null || elapsed > 0L)) {
            // The retained locator is the recreation handoff. Persisting the old Activity's final
            // location on a separate IO coroutine could otherwise overwrite a newer location.
            val locationToPersist = currentLocation.takeUnless { isChangingConfigurations }
            lifecycleScope.launchNonCancellable {
                locationToPersist?.let { locator ->
                    session.saveLocation(
                        locator,
                        completed = isEpubPublicationComplete(
                            locator,
                            session.publicationSession.publication.readingOrder,
                        ),
                    )
                }
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
        navigationController.close()
        childWebViewResolver.close()
        selectionCoordinator?.close()
        selectionCoordinator = null
        translationController?.close()
        translationController = null
        inputListener?.let { listener -> navigator?.removeInputListener(listener) }
        inputListener = null
        super.onDestroy()
        openedSession = null
        readerHost = null
        navigator = null
        settings = null
    }

    private fun showReader(
        session: OpenedBookReaderSession,
        readerSettings: ReadiumEpubSettingsBinding,
        initialPreferences: EpubPreferences,
    ): Boolean {
        val publicationSession = session.publicationSession as? ReadiumPublicationSession
            ?: run {
                showError(getString(R.string.book_reader_incompatible_session))
                return false
            }
        val host = ReadiumEpubReaderHost(publicationSession)
        val activeTranslationController = BookSelectionTranslationController(
            feature = Injekt.get<TranslationFeature>(),
            hostActions = Injekt.get<TranslationHostActions>(),
            automaticSelectionEnabled = Injekt.get<BookAutomaticTranslationSettingsProvider>()
                .automaticSelectionEnabled(readerSettings.readerSettingsSurfaceId),
            scope = lifecycleScope,
            initialCapabilities = session.readerCapabilities,
        ).also { translationController = it }
        val activeSelectionCoordinator = ReadiumEpubSelectionCoordinator(
            activity = this,
            scope = lifecycleScope,
            controller = activeTranslationController,
            navigator = { navigator },
            isFixedLayout = { readerHost?.isFixedLayout },
            readerRootPositionInWindow = { readerRootPositionInWindow },
        ).also { selectionCoordinator = it }
        val selectionChangeBridge = activeSelectionCoordinator.changeBridge
        val paginationListener = object : EpubNavigatorFragment.PaginationListener {
            override fun onPageChanged(pageIndex: Int, totalPages: Int, locator: Locator) {
                navigationController.onPageChanged(pageIndex, totalPages, locator)
            }

            override fun onPageLoaded() {
                activeSelectionCoordinator.clearSelection()
                navigationController.onPageLoaded()
                activeSelectionCoordinator.installListener()
            }
        }
        val fragmentFactory = host.createFragmentFactory(
            initialLocator = retainedSession.currentLocator ?: session.initialLocator,
            initialPreferences = initialPreferences,
            paginationListener = paginationListener,
            selectionChangeBridge = selectionChangeBridge,
        )
        supportFragmentManager.fragmentFactory = fragmentFactory
        val fragment = fragmentFactory.instantiate(
            classLoader,
            EpubNavigatorFragment::class.java.name,
        ) as EpubNavigatorFragment
        supportFragmentManager.commitNow {
            replace(containerId, fragment)
        }
        navigationController.effectiveReadingDirection = host.readingDirection(fragment)

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
            readingDirection = navigationController.effectiveReadingDirection,
            backgroundColor = host.backgroundColor(fragment),
        )
        resolveChildWebView(session)
        inputListener = navigationController
            .createInputListener(navigationController.effectiveReadingDirection)
            .also(fragment::addInputListener)
        readingStartedAt = SystemClock.elapsedRealtime()
        host.observeLocations(fragment, lifecycleScope) { locator ->
            retainedSession.updateLocation(locator)
            session.saveLocation(
                locator,
                completed = isEpubPublicationComplete(locator, publication.readingOrder),
            )
            navigationController.updateBookLocation(locator)
        }
        host.observeSettings(fragment, readerSettings, lifecycleScope)
        host.observeBackgroundColor(fragment, lifecycleScope) { backgroundColor ->
            uiState = uiState.copy(backgroundColor = backgroundColor)
        }
        activeSelectionCoordinator.installListener()
        readerContainer.visibility = View.VISIBLE
        surfaceState = ReaderSurfaceState.Ready
        setMenuVisibility(false)
        return true
    }

    override fun onProvideAssistContent(outContent: AssistContent) {
        super.onProvideAssistContent(outContent)
        uiState.childWebView?.url?.let { outContent.webUri = Uri.parse(it) }
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

    internal fun setMenuVisibility(visible: Boolean) {
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
        private const val EXTRA_SESSION_TOKEN = "session_token"
        fun newIntent(
            context: Context,
            request: BookReaderRequest,
            processorId: String,
            sessionToken: String,
        ): Intent = Intent(context, ReadiumEpubReaderActivity::class.java).apply {
            putExtra(EXTRA_ENTRY_ID, request.entryId)
            putExtra(EXTRA_CHAPTER_ID, request.chapterId)
            putExtra(EXTRA_PROCESSOR_ID, processorId)
            putExtra(EXTRA_SESSION_TOKEN, sessionToken)
        }
    }
}
