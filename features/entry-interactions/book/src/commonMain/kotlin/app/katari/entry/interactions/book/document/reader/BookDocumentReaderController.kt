package app.katari.entry.interactions.book.document.reader

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.katari.book.api.BookFailureReason
import app.katari.entry.interactions.book.document.preparation.PreparedBookDocumentPublication
import app.katari.entry.interactions.book.document.reader.font.BookDocumentReaderFonts
import app.katari.entry.interactions.book.document.reader.handoff.BookProgressOffer
import app.katari.entry.interactions.book.document.reader.handoff.bookProgressOffer
import app.katari.entry.interactions.book.document.reader.input.BookDocumentReaderKeys
import app.katari.entry.interactions.book.document.reader.navigation.BookDocumentNavigationTarget
import app.katari.entry.interactions.book.document.reader.search.BookDocumentBookSearch
import app.katari.entry.interactions.book.document.reader.settings.BookDocumentReaderSettingBindings
import app.katari.entry.interactions.book.document.resource.BookRemoteResourceConsentCoordinator
import app.katari.entry.interactions.book.document.resource.PendingBookRemoteResourceConsent
import app.katari.entry.interactions.book.navigation.BookChapterReadingOrder
import app.katari.entry.interactions.book.preparation.BookOpeningProgress
import app.katari.entry.interactions.book.preparation.BookRemoteResourceAuthorization
import app.katari.entry.interactions.book.processor.BookReaderRequest
import app.katari.entry.interactions.book.reader.BookChildWebViewResolver
import app.katari.entry.interactions.book.reader.BookReaderDependencies
import app.katari.entry.interactions.book.reader.BookReaderOpenResult
import app.katari.entry.interactions.book.reader.OpenedBookReaderSession
import app.katari.entry.interactions.book.reader.SourceChapter
import app.katari.entry.interactions.book.reader.failure.BookReaderChapterHeading
import app.katari.entry.interactions.book.reader.language.BookSelectionLanguageSession
import app.katari.entry.interactions.book.reader.selection.BookSelectionActionCoordinator
import app.katari.entry.interactions.book.reader.speech.BookShortFormSpeechController
import app.katari.entry.interactions.book.reader.translation.BookSelectionTranslationController
import app.katari.entry.interactions.book.reader.translation.BookTranslationLanguageStore
import app.katari.entry.interactions.reader.navigation.EntryReaderNavigationPresenter
import app.katari.entry.interactions.reader.settings.BookDocumentReaderTheme
import app.katari.entry.interactions.reader.settings.BookTextRule
import app.katari.entry.interactions.source.EntryChildWebViewAction
import app.katari.entry.interactions.source.EntryChildWebViewResolution
import app.katari.presentation.kit.feedback.SnackbarHostState
import app.katari.tts.ui.playback.ShortFormSpeechFailure
import dev.icerock.moko.resources.StringResource
import eu.kanade.tachiyomi.source.entry.EntryType
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import logcat.LogPriority
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.resubscribeOnFailure
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.model.ChapterGroupId
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.i18n.*
import tachiyomi.i18n.platform.stringResource

/**
 * The book reader for as long as its window shows it: opening the book, the chapters around the one being read, the
 * contents, bookmarks, links, search across chapters, the selection's translation and speech, and the dialogs those
 * raise. Each platform's host makes one in its window's [scope], forwards the window's lifecycle to it, starts it
 * with [start], and shows it with [BookDocumentReaderContent]. [retainedSessions] outlives it, as when Android
 * recreates the activity or a platform reopens the reader through [BookDocumentReaderHost.onReopen].
 */
@Stable
internal class BookDocumentReaderController(
    request: BookReaderRequest,
    /** Hands over the session the open flow prepared; null when the reader opens its chapter itself. */
    private val sessionToken: String?,
    private val retainedSessions: BookDocumentReaderSessionViewModel,
    private val dependencies: BookReaderDependencies,
    private val host: BookDocumentReaderHost,
    private val scope: CoroutineScope,
) {
    // A reader opened again after the process died reopens the chapter it had moved on to.
    private val startupRequest = BookReaderRequest(
        entryId = request.entryId,
        chapterId = retainedSessions.restoredChapterId ?: request.chapterId,
    )

    var surfaceState by mutableStateOf<BookDocumentReaderSurfaceState>(BookDocumentReaderSurfaceState.Loading)
        private set

    /** The step opening is on while [surfaceState] is loading; null until the first step reports. */
    var openingProgress by mutableStateOf<BookOpeningProgress?>(null)
        private set
    var readerState by mutableStateOf<BookDocumentReaderState?>(null)
        private set
    val visualChapterProgression = mutableFloatStateOf(0f)

    var settingBindings by mutableStateOf<BookDocumentReaderSettingBindings?>(null)
        private set
    var readerFonts by mutableStateOf<BookDocumentReaderFonts?>(null)
        private set
    var selectionCoordinator by mutableStateOf<BookSelectionActionCoordinator?>(null)
        private set

    /** Whether the reader's setting keeps the screen on while it shows. */
    var keepsScreenAwake by mutableStateOf(false)
        private set

    /** What the system bars should show; null while the reader shows no book, when the app's own bars return. */
    var systemBars by mutableStateOf<BookDocumentReaderSystemBarsRequest?>(null)
        private set

    var pendingExternalLink by mutableStateOf<String?>(null)
        private set
    var pendingHiddenText by mutableStateOf<String?>(null)
        private set
    var pendingRemoteResourceConsent by mutableStateOf<PendingBookRemoteResourceConsent?>(null)
        private set

    /** Where another device got to, offered until it is taken, dismissed or the page moves (D053). */
    var progressOffer by mutableStateOf<BookProgressOffer?>(null)
        private set

    val snackbarHostState = SnackbarHostState()
    val keys = BookDocumentReaderKeys()
    val jumpHistory get() = retainedSessions.jumpHistory

    /** Whether back would go to where a link or footnote was followed from, before anything else. */
    val returnsFromJump: Boolean
        get() = readerState != null && jumpHistory.returnTarget != null

    private var startupJob: Job? = null
    private var navigationPresentationJob: Job? = null
    private val remoteResourceConsentCoordinator =
        BookRemoteResourceConsentCoordinator(dependencies.remoteResourceConsentPreferences)
    private val navigationPresenter = EntryReaderNavigationPresenter(
        getEntryWithChapters = dependencies.getEntryWithChapters,
        childListFeature = dependencies.childListFeature,
    )
    private val childWebViewResolver = BookChildWebViewResolver(
        scope = scope,
        feature = dependencies.webViewFeature,
        currentChapterId = { retainedSessions.currentChapterId },
        onResolution = { resolution -> readerState = readerState?.copy(childWebView = resolution) },
        onFailure = { error ->
            logcat(LogPriority.ERROR, error) { "Failed to resolve BOOK child WebView URL" }
        },
    )
    val chapterCoordinator = BookDocumentChapterCoordinator(
        context = host.context,
        scope = scope,
        retainedSessions = retainedSessions,
        sessionFactory = dependencies.sessionFactory,
        isNextChapterPreparationEnabled = {
            settingBindings?.prepareNextChapter?.state?.value?.effectiveValue == true
        },
        currentState = { readerState },
        updateState = { readerState = it },
        updateVisualChapterProgression = { visualChapterProgression.floatValue = it },
        onNavigationMissing = { showReaderFeedback(MR.strings.book_document_anchor_missing) },
        onSessionActivated = { session ->
            selectionCoordinator?.updateCapabilities(session.readerCapabilities)
            childWebViewResolver.resolve(session)
            promptForRemoteResources(session) {}
        },
    )
    val bookSearch = BookDocumentBookSearch(
        chapters = { readerState?.readingOrder?.chapters.orEmpty() },
        openPublications = {
            readerState?.loadedSections?.keys.orEmpty().mapNotNull { chapterId ->
                (retainedSessions.session(chapterId)?.preparedPublication as? PreparedBookDocumentPublication)
                    ?.let { chapterId to it }
            }.toMap()
        },
        openDownloaded = { chapter ->
            retainedSessions.currentSession()?.let { current ->
                dependencies.sessionFactory.openDownloaded(
                    BookReaderRequest(current.entry.id, chapter.id),
                    BookDocumentReaderProcessor.PROCESSOR_ID,
                )
            }
        },
    )

    private fun watchProgressOffers() {
        scope.launch {
            dependencies.progressOffers.offers(setOf(startupRequest.entryId)).collect { offer ->
                val state = readerState ?: return@collect
                bookProgressOffer(
                    offer = offer,
                    chapters = state.readingOrder.chapters,
                    currentChapterId = state.currentChapterId,
                    progression = visualChapterProgression.floatValue.toDouble(),
                )?.let { progressOffer = it }
            }
        }
    }

    fun goToProgressOffer() {
        val offer = progressOffer ?: return
        progressOffer = null
        chapterCoordinator.selectNavigationTarget(
            BookDocumentNavigationTarget(offer.chapter, offer.locator, restorePosition = true),
        )
    }

    fun dismissProgressOffer() {
        progressOffer = null
    }

    fun start() {
        watchProgressOffers()
        if (startupRequest.entryId < 0L || startupRequest.chapterId < 0L) {
            showError(host.context.stringResource(MR.strings.book_reader_invalid_request))
        } else {
            startOpen()
        }
    }

    fun onStart() = chapterCoordinator.startReading()

    fun onResume() {
        selectionCoordinator?.onResume()
    }

    /** [persist] saves where the reader is; Android leaves it out while it recreates the activity. */
    fun onStop(persist: Boolean) {
        selectionCoordinator?.onStop()
        chapterCoordinator.stopReading(persist = persist)
    }

    fun close() {
        startupJob?.cancel()
        navigationPresentationJob?.cancel()
        chapterCoordinator.close()
        childWebViewResolver.close()
        selectionCoordinator?.close()
        selectionCoordinator = null
    }

    suspend fun loadFailureHeading(): BookReaderChapterHeading? = dependencies.chapterHeadingLoader.load(startupRequest)

    fun resolveFailureWebView(sourceChapter: SourceChapter): EntryChildWebViewResolution.Available? =
        dependencies.webViewFeature.resolveChild(sourceChapter.owner, sourceChapter.chapter)
            as? EntryChildWebViewResolution.Available

    fun retryOpen() {
        val error = surfaceState as? BookDocumentReaderSurfaceState.Error ?: return
        if (!error.canRetry) return
        startOpen()
    }

    /**
     * Closes what is open over the reader, the innermost first; false when nothing is, so back leaves the reader.
     */
    fun handleBack(): Boolean {
        val state = readerState
        when {
            pendingExternalLink != null -> pendingExternalLink = null
            pendingRemoteResourceConsent != null -> resolveRemoteResourceConsent(allow = false)
            state?.navigationVisible == true -> setNavigationVisible(false)
            state?.settingsVisible == true -> setSettingsVisible(false)
            state?.auxiliarySection != null -> chapterCoordinator.dismissAuxiliarySection()
            else -> return false
        }
        return true
    }

    val handlesBack: Boolean
        get() = pendingExternalLink != null ||
            pendingRemoteResourceConsent != null ||
            readerState?.let { it.navigationVisible || it.settingsVisible || it.auxiliarySection != null } == true

    fun setSettingsVisible(visible: Boolean) {
        readerState = readerState?.copy(settingsVisible = visible)
    }

    fun setNavigationVisible(visible: Boolean) {
        readerState = readerState?.let { state ->
            state.copy(
                navigationVisible = visible,
                navigationLocator = retainedSessions.locator(state.currentChapterId),
            )
        }
        if (!visible) {
            navigationPresentationJob?.cancel()
            navigationPresentationJob = null
            return
        }
        val entry = retainedSessions.currentSession()?.entry ?: return
        val readingOrder = readerState?.readingOrder ?: return
        observeNavigationPresentation(entry, readingOrder)
    }

    fun returnToJumpOrigin() {
        retainedSessions.jumpHistory.returnTarget?.let {
            chapterCoordinator.selectNavigationTarget(it.copy(returnToOrigin = true))
        }
    }

    fun selectNavigationTarget(target: BookDocumentNavigationTarget) = chapterCoordinator.selectNavigationTarget(target)

    fun toggleChrome() = setChromeVisible(readerState?.chromeVisible != true)

    fun setChromeVisible(visible: Boolean) {
        if (readerState?.chromeVisible != visible) {
            readerState = readerState?.copy(chromeVisible = visible)
        }
        updateSystemBars()
    }

    fun toggleCurrentChapterBookmark() {
        val state = readerState ?: return
        val owner = retainedSessions.currentSession()?.owner ?: return
        val wasBookmarked = state.currentChapterId in state.bookmarkedChapterIds
        val chapter = state.window.current.copy(bookmark = wasBookmarked)
        readerState = state.copy(
            bookmarkedChapterIds = if (wasBookmarked) {
                state.bookmarkedChapterIds - chapter.id
            } else {
                state.bookmarkedChapterIds + chapter.id
            },
        )
        scope.launchNonCancellable {
            dependencies.bookmarkFeature.setBookmarked(owner, listOf(chapter), !wasBookmarked)
        }
    }

    fun launchChildWebViewAction(action: EntryChildWebViewAction, resolution: EntryChildWebViewResolution.Available) {
        host.onChildWebViewAction(action, resolution, retainedSessions.currentSession()?.entry?.displayTitle)
    }

    fun confirmExternalLink(url: String) {
        if (externalLinkHost(url) == null) {
            showReaderFeedback(MR.strings.book_document_external_link_unavailable)
            return
        }
        pendingExternalLink = url
    }

    fun openPendingExternalLink() {
        val url = pendingExternalLink ?: return
        pendingExternalLink = null
        dependencies.linkOpener.open(host.context, url)
    }

    fun dismissExternalLink() {
        pendingExternalLink = null
    }

    fun requestHideText(text: String) {
        pendingHiddenText = text
    }

    fun dismissHideText() {
        pendingHiddenText = null
    }

    /** The chapter being read, whose book and source the dialog that hides text offers; null before one opens. */
    fun currentSession(): OpenedBookReaderSession? = retainedSessions.currentSession()

    /** Chapters take text rules as they open, so the open ones open again, in place, with the new rules. */
    fun addTextRules(rules: List<BookTextRule>) {
        pendingHiddenText = null
        val preference = dependencies.settingsProvider.textRules
        preference.set(preference.get() + rules)
        chapterCoordinator.stopReading(persist = true)
        retainedSessions.closeSessionsToReopen()
        host.onReopen()
    }

    fun resolveRemoteResourceConsent(allow: Boolean) {
        val pending = pendingRemoteResourceConsent ?: return
        pendingRemoteResourceConsent = null
        remoteResourceConsentCoordinator.resolve(pending, allow)
    }

    fun showReaderFeedback(message: StringResource) {
        scope.launch {
            snackbarHostState.showSnackbar(host.context.stringResource(message))
        }
    }

    private suspend fun installSettingBindings(entryId: Long) {
        if (settingBindings != null) return
        val bindings = BookDocumentReaderSettingBindings.create(
            provider = dependencies.settingsProvider,
            binder = dependencies.viewerSettingBinder,
            entryId = entryId,
        )
        settingBindings = bindings
        scope.launch {
            bindings.readingMode.state.collect { retainedSessions.readingMode = it.effectiveValue }
        }
        scope.launch {
            bindings.keepScreenAlive.state
                .map { setting -> setting.effectiveValue }
                .distinctUntilChanged()
                .collect { keepsScreenAwake = it }
        }
        scope.launch {
            bindings.prepareNextChapter.state
                .map { setting -> setting.effectiveValue }
                .distinctUntilChanged()
                .collect { enabled ->
                    if (enabled) chapterCoordinator.prepareCurrentNextChapterIfNeeded()
                }
        }
        listOf(bindings.showStatusBar.state, bindings.showNavigationBar.state, bindings.theme.state).forEach { state ->
            scope.launch {
                state
                    .map { setting -> setting.effectiveValue }
                    .distinctUntilChanged()
                    .collect { updateSystemBars() }
            }
        }
    }

    private fun startOpen() {
        startupJob?.cancel()
        navigationPresentationJob?.cancel()
        surfaceState = BookDocumentReaderSurfaceState.Loading
        openingProgress = null
        startupJob = scope.launch {
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
        val handedOff = if (retained == null && !sessionToken.isNullOrBlank()) {
            dependencies.sessionRegistry.claim(sessionToken, startupRequest)
        } else {
            null
        }
        // Null where the session goes on in a rebuilt reader. A reader that opens its chapter itself, as after its
        // process died, starts its session's report here.
        val report = if (retained == null) {
            handedOff?.report ?: dependencies.readingSessionReports.start(EntryType.BOOK)
        } else {
            null
        }
        val result = when (val existing = retained ?: handedOff?.session) {
            null -> dependencies.sessionFactory.open(
                startupRequest,
                BookDocumentReaderProcessor.PROCESSOR_ID,
            ) { openingProgress = it }
            else -> BookReaderOpenResult.Success(existing)
        }
        when (result) {
            is BookReaderOpenResult.Failure -> showError(
                message = result.failure.message,
                canRetry = result.canRetry,
                reason = result.failure.reason,
                sourceChapter = result.sourceChapter,
            )
            is BookReaderOpenResult.Success -> {
                report?.let { retainedSessions.attachInitial(result.session, it) }
                showInitialSession(result.session)
            }
        }
    }

    private suspend fun showInitialSession(session: OpenedBookReaderSession) {
        if (promptForRemoteResources(session) { scope.launch { showAuthorizedInitialSession(session) } }) return
        showAuthorizedInitialSession(session)
    }

    private suspend fun showAuthorizedInitialSession(session: OpenedBookReaderSession) {
        val readingOrder = BookChapterReadingOrder(
            dependencies.chapterNavigationResolver.resolveAll(session.entry, session.chapter.id),
        )
        val window = readingOrder.window(session.chapter.id)
            ?: return showError(host.context.stringResource(MR.strings.book_document_chapter_missing))
        val sections = session.toDocumentSections(retainedSessions.locator(session.chapter.id))
            ?: return showError(
                host.context.stringResource(MR.strings.book_document_incompatible),
                reason = BookFailureReason.FORMAT_UNSUPPORTED,
            )
        ensureSelectionCoordinator(session)
        val section = sections.initialSection
        val progression = section.totalProgression(
            section.document.document.progressionAt(section.initialPosition),
        )
        visualChapterProgression.floatValue = progression
        val fonts = readerFonts ?: BookDocumentReaderFonts(
            store = dependencies.fontStore,
            documents = dependencies.storageFiles,
            decoding = dependencies.resourceDecoding,
            profileId = session.entry.profileId,
        ).also { readerFonts = it }
        fonts.refresh()
        val chapterGroupTitles = readingOrder.chapters.map(EntryChapter::entryId).distinct()
            .flatMap { entryId -> dependencies.chapterGroupRepository.getByEntryId(entryId) }
            .associate { ChapterGroupId(it.entryId, it.key) to it.title }
        readerState = BookDocumentReaderState(
            entryTitle = session.entry.displayTitle,
            readingOrder = readingOrder,
            currentChapterId = session.chapter.id,
            window = window,
            bookmarkedChapterIds = readingOrder.chapters
                .filter(EntryChapter::bookmark)
                .mapTo(mutableSetOf(), EntryChapter::id),
            chapterGroupTitles = chapterGroupTitles,
            loadedSections = mapOf(session.chapter.id to sections),
            publicationNavigation = mapOf(session.chapter.id to session.preparedPublication.publication.navigation),
        )
        childWebViewResolver.resolve(session)
        surfaceState = BookDocumentReaderSurfaceState.Ready
        setChromeVisible(false)
        chapterCoordinator.startReading()
        chapterCoordinator.prepareNextChapterIfNeeded(progression.toDouble())
    }

    private fun promptForRemoteResources(
        session: OpenedBookReaderSession,
        onResolved: () -> Unit,
    ): Boolean {
        val authorization = session.preparedPublication as? BookRemoteResourceAuthorization ?: return false
        if (authorization.remoteResourceRequests.isEmpty()) return false
        val publicationId = session.preparedPublication.publication.id
        pendingRemoteResourceConsent = remoteResourceConsentCoordinator.pendingConsent(
            publicationId = publicationId,
            authorization = authorization,
            onResolved = onResolved,
        ) ?: return false
        return true
    }

    private fun observeNavigationPresentation(entry: Entry, readingOrder: BookChapterReadingOrder) {
        navigationPresentationJob?.cancel()
        navigationPresentationJob = scope.launch {
            navigationPresenter.observe(entry, readingOrder.chapters)
                .resubscribeOnFailure(this@BookDocumentReaderController)
                .collect { presentation ->
                    readerState = readerState?.copy(navigationPresentation = presentation)
                }
        }
    }

    private fun ensureSelectionCoordinator(session: OpenedBookReaderSession) {
        if (session.readerSettingsSurfaceId != BookDocumentReaderProcessor.SETTINGS_SURFACE_ID) return
        val automaticTranslation = settingBindings?.automaticTranslation ?: return
        val languageSession = BookSelectionLanguageSession(session.preparedPublication.publication.languages)
        val translationController = BookSelectionTranslationController(
            feature = dependencies.translationFeature,
            hostActions = dependencies.translationHostActions,
            automaticSelectionSetting = automaticTranslation.state,
            languageSession = languageSession,
            scope = scope,
            initialCapabilities = session.readerCapabilities,
            languageStore = BookTranslationLanguageStore(
                feature = dependencies.translationLanguagesFeature,
                book = retainedSessions.currentOwner,
            ),
        )
        selectionCoordinator = BookSelectionActionCoordinator(
            translationController = translationController,
            speechController = BookShortFormSpeechController(
                feature = dependencies.ttsFeature,
                scope = scope,
                onFailure = ::showSpeechFailure,
                onLanguageResolved = languageSession::record,
            ),
            languageSession = languageSession,
            scope = scope,
            initialCapabilities = session.readerCapabilities,
        )
    }

    private fun showSpeechFailure(failure: ShortFormSpeechFailure) {
        val message = when (failure) {
            ShortFormSpeechFailure.LanguageUnavailable ->
                MR.strings.book_reader_selection_speech_language_unavailable
            ShortFormSpeechFailure.ConfigurationRequired ->
                MR.strings.book_reader_selection_speech_configuration_required
            is ShortFormSpeechFailure.Unavailable ->
                MR.strings.book_reader_selection_speech_unavailable
            is ShortFormSpeechFailure.PlaybackFailed ->
                MR.strings.book_reader_selection_speech_failed
        }
        host.showMessage(failure.explanation ?: host.context.stringResource(message))
    }

    private fun updateSystemBars() {
        if (surfaceState != BookDocumentReaderSurfaceState.Ready) return
        val bindings = settingBindings
        systemBars = BookDocumentReaderSystemBarsRequest(
            chromeVisible = readerState?.chromeVisible == true,
            keepStatusBarVisible = bindings?.showStatusBar?.state?.value?.effectiveValue == true,
            keepNavigationBarVisible = bindings?.showNavigationBar?.state?.value?.effectiveValue == true,
            readerTheme = bindings?.theme?.state?.value?.effectiveValue ?: BookDocumentReaderTheme.App,
        )
    }

    private fun showError(
        message: String,
        canRetry: Boolean = false,
        reason: BookFailureReason = BookFailureReason.CONTENT_UNAVAILABLE,
        sourceChapter: SourceChapter? = null,
    ) {
        systemBars = null
        surfaceState = BookDocumentReaderSurfaceState.Error(reason, message, canRetry, sourceChapter)
    }
}

internal sealed interface BookDocumentReaderSurfaceState {
    data object Loading : BookDocumentReaderSurfaceState
    data object Ready : BookDocumentReaderSurfaceState
    data class Error(
        val reason: BookFailureReason,
        val message: String,
        val canRetry: Boolean,
        val sourceChapter: SourceChapter?,
    ) : BookDocumentReaderSurfaceState
}

/** What the reader asks of the system bars while it shows a book, which a platform without them leaves alone. */
internal data class BookDocumentReaderSystemBarsRequest(
    val chromeVisible: Boolean,
    val keepStatusBarVisible: Boolean,
    val keepNavigationBarVisible: Boolean,
    val readerTheme: BookDocumentReaderTheme,
)

/** The site an external link opens, for the dialog that confirms it; null for anything but a web link. */
internal fun externalLinkHost(url: String): String? =
    WEB_LINK.find(url.trim())?.groupValues?.get(1)?.takeIf { it.isNotBlank() }

// Scheme, then an optional user part, then the host up to the port, path, query or fragment.
private val WEB_LINK = Regex("""^https?://(?:[^/?#@]*@)?(\[[^\]]*]|[^/?#:]+)""", RegexOption.IGNORE_CASE)
