package eu.kanade.tachiyomi.ui.reader

import android.app.Application
import android.net.Uri
import androidx.annotation.IntRange
import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import eu.kanade.tachiyomi.data.database.models.toDomainChapter
import eu.kanade.tachiyomi.source.model.Page
import eu.kanade.tachiyomi.ui.reader.loader.ChapterLoader
import eu.kanade.tachiyomi.ui.reader.model.InsertPage
import eu.kanade.tachiyomi.ui.reader.model.ReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.ReaderPage
import eu.kanade.tachiyomi.ui.reader.model.ViewerChapters
import eu.kanade.tachiyomi.ui.reader.model.filterDownloaded
import eu.kanade.tachiyomi.ui.reader.model.ref
import eu.kanade.tachiyomi.ui.reader.model.removeDuplicates
import eu.kanade.tachiyomi.ui.reader.model.toEntryChapter
import eu.kanade.tachiyomi.ui.reader.model.toReaderChapter
import eu.kanade.tachiyomi.ui.reader.model.unref
import eu.kanade.tachiyomi.ui.reader.viewer.Viewer
import eu.kanade.tachiyomi.util.lang.byteSize
import eu.kanade.tachiyomi.util.storage.DiskUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.runBlocking
import logcat.LogPriority
import mihon.entry.interactions.EntryBookmarkFeature
import mihon.entry.interactions.EntryChildWebViewResolution
import mihon.entry.interactions.EntryMediaSessionActivity
import mihon.entry.interactions.EntryMediaSessionEvent
import mihon.entry.interactions.EntryWebViewFeature
import mihon.entry.interactions.manga.MangaMediaSessionProcessor
import mihon.entry.interactions.manga.download.DownloadManager
import mihon.entry.interactions.manga.download.DownloadProvider
import mihon.entry.interactions.manga.download.model.MangaDownload
import mihon.entry.interactions.manga.mangaProgressState
import mihon.entry.interactions.manga.pageIndex
import mihon.entry.interactions.reader.settings.MangaReaderSettingsProvider
import mihon.entry.interactions.reader.settings.ReaderOrientation
import mihon.entry.interactions.reader.settings.ReadingMode
import mihon.entry.viewer.settings.ResolvedViewerSetting
import mihon.entry.viewer.settings.ViewerSettingBinder
import mihon.entry.viewer.settings.ViewerSettingBinding
import mihon.entry.viewer.settings.ViewerSettingSource
import tachiyomi.core.common.preference.toggle
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.core.common.util.lang.launchNonCancellable
import tachiyomi.core.common.util.lang.withIOContext
import tachiyomi.core.common.util.lang.withUIContext
import tachiyomi.core.common.util.system.logcat
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.interactor.GetEntryWithChapters
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.entry.model.EntryChapter
import tachiyomi.domain.entry.model.progressResourceKey
import tachiyomi.domain.entry.repository.EntryProgressRepository
import tachiyomi.domain.entry.repository.EntryRepository
import tachiyomi.domain.entry.service.sortedForReading
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.source.local.LocalSource
import tachiyomi.source.local.image.LocalCoverManager
import tachiyomi.source.local.isLocal
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get
import java.io.File
import java.io.InputStream
import java.time.Instant
import java.util.Date

/**
 * Presenter used by the activity to perform background operations.
 */
internal class ReaderViewModel @JvmOverloads constructor(
    private val savedState: SavedStateHandle,
    private val sourceManager: SourceManager = Injekt.get(),
    private val webViewFeature: EntryWebViewFeature = Injekt.get(),
    private val imageSaver: ReaderImageSaver = ReaderImageSaver(Injekt.get<Application>()),
    val readerPreferences: MangaReaderSettingsProvider = Injekt.get(),
    private val getEntry: GetEntry = Injekt.get(),
    private val getEntryWithChapters: GetEntryWithChapters = Injekt.get(),
    private val entryProgressRepository: EntryProgressRepository = Injekt.get(),
    private val entryRepository: EntryRepository = Injekt.get(),
    private val bookmarkFeature: EntryBookmarkFeature = Injekt.get(),
    private val viewerSettingBinder: ViewerSettingBinder = Injekt.get(),
    private val libraryPreferences: LibraryPreferences = Injekt.get(),
    private val localCoverManager: LocalCoverManager = Injekt.get(),
    private val mediaSession: MangaMediaSessionProcessor = Injekt.get(),
) : ViewModel() {
    private val downloadManager: DownloadManager = Injekt.get()
    private val downloadProvider: DownloadProvider = Injekt.get()

    private val mutableState = MutableStateFlow(State())
    val state = mutableState.asStateFlow()

    private val eventChannel = Channel<Event>()
    val eventFlow = eventChannel.receiveAsFlow()

    /**
     * The manga loaded in the reader. It can be null when instantiated for a short time.
     */
    val manga: Entry?
        get() = state.value.manga

    /**
     * The chapter id of the currently loaded chapter. Used to restore from process kill.
     */
    private var chapterId = savedState.get<Long>("chapter_id") ?: -1L
        set(value) {
            savedState["chapter_id"] = value
            field = value
        }

    /**
     * The visible page index of the currently loaded chapter. Used to restore from process kill.
     */
    private var chapterPageIndex = savedState.get<Int>("page_index") ?: -1
        set(value) {
            savedState["page_index"] = value
            field = value
        }

    private var initialPageIndexPending = chapterPageIndex >= 0

    /**
     * The chapter loader for the loaded manga. It'll be null until [manga] is set.
     */
    private var loader: ChapterLoader? = null

    /**
     * The time the chapter was started reading
     */
    private var chapterReadStartTime: Long? = null

    private var chapterToDownload: MangaDownload? = null
    private var readingModeBinding: ViewerSettingBinding<Int>? = null
    private var orientationBinding: ViewerSettingBinding<Int>? = null

    private val unfilteredChapterList by lazy {
        val manga = manga!!
        runBlocking { getAllEntryChapters(manga, applyScanlatorFilter = false) }
    }

    /**
     * Chapter list for the active manga. It's retrieved lazily and should be accessed for the first
     * time in a background thread to avoid blocking the UI.
     */
    private val chapterList by lazy {
        val manga = manga!!
        val entry = runBlocking { getEntry.await(manga.id) }
            ?: error("Entry ${manga.id} not found")
        val chapters = runBlocking { getAllEntryChapters(manga, applyScanlatorFilter = true) }
        val mergedEntryIds = chapters.map { it.entryId }.distinct()
        val progressByChapterId = mergedEntryIds
            .flatMap { entryId -> runBlocking { entryProgressRepository.getByEntryId(entryId) } }
            .associateBy { it.chapterId }
        val mangaById = mergedEntryIds.associateWith { entryId ->
            runBlocking { getEntry.await(entryId) }
        }

        val selectedChapter = chapters.find { it.id == chapterId }
            ?: error("Requested chapter of id $chapterId not found in chapter list")

        val chaptersForReader = when {
            (readerPreferences.skipRead.get() || readerPreferences.skipFiltered.get()) -> {
                val filteredChapters = chapters.filterNot {
                    when {
                        readerPreferences.skipRead.get() && it.read -> true
                        readerPreferences.skipFiltered.get() -> {
                            val chapterManga = mangaById[it.entryId] ?: manga
                            (manga.unreadFilterRaw == Entry.CHAPTER_SHOW_READ && !it.read) ||
                                (manga.unreadFilterRaw == Entry.CHAPTER_SHOW_UNREAD && it.read) ||
                                (
                                    manga.downloadedFilterRaw == Entry.CHAPTER_SHOW_DOWNLOADED &&
                                        !downloadManager.isChapterDownloaded(
                                            it.name,
                                            it.scanlator,
                                            it.url,
                                            chapterManga.title,
                                            chapterManga.source,
                                        )
                                    ) ||
                                (
                                    manga.downloadedFilterRaw == Entry.CHAPTER_SHOW_NOT_DOWNLOADED &&
                                        downloadManager.isChapterDownloaded(
                                            it.name,
                                            it.scanlator,
                                            it.url,
                                            chapterManga.title,
                                            chapterManga.source,
                                        )
                                    ) ||
                                (manga.bookmarkedFilterRaw == Entry.CHAPTER_SHOW_BOOKMARKED && !it.bookmark) ||
                                (manga.bookmarkedFilterRaw == Entry.CHAPTER_SHOW_NOT_BOOKMARKED && it.bookmark)
                        }
                        else -> false
                    }
                }

                if (filteredChapters.any { it.id == chapterId }) {
                    filteredChapters
                } else {
                    filteredChapters + listOf(selectedChapter)
                }
            }
            else -> chapters
        }

        chaptersForReader
            .sortedForReading(entry, mergedEntryIds)
            .run {
                val domainChapters = map { it.toReaderChapter() }
                if (readerPreferences.skipDupe.get()) {
                    domainChapters.removeDuplicates(selectedChapter.toReaderChapter())
                } else {
                    domainChapters
                }
            }
            .run {
                if (libraryPreferences.downloadedOnly.get()) {
                    filterDownloaded(mangaById.filterValues { it != null }.mapValues { it.value!! })
                } else {
                    this
                }
            }
            .map { domainChapter ->
                val originManga = mangaById[domainChapter.mangaId]
                ReaderChapter(
                    chapter = domainChapter,
                    manga = originManga,
                ).apply {
                    requestedPage = progressByChapterId[domainChapter.id]?.pageIndex?.toInt() ?: 0
                }
            }
    }

    init {
        // To save state
        state.map { it.viewerChapters?.current }
            .distinctUntilChanged()
            .filterNotNull()
            .onEach { currentChapter ->
                if (initialPageIndexPending && chapterPageIndex >= 0) {
                    // Restore from SavedState
                    currentChapter.requestedPage = chapterPageIndex
                    initialPageIndexPending = false
                }
                chapterId = currentChapter.chapter.id!!
            }
            .launchIn(viewModelScope)
    }

    override fun onCleared() {
        val currentChapters = state.value.viewerChapters
        if (currentChapters != null) {
            currentChapters.unref()
            chapterToDownload?.let {
                downloadManager.addDownloadsToStartOfQueue(listOf(it))
            }
        }
    }

    /**
     * Called when the user pressed the back button and is going to leave the reader. Used to
     * trigger deletion of the downloaded chapters.
     */
    fun onActivityFinish() {
        deletePendingChapters()
    }

    /**
     * Whether this presenter is initialized yet.
     */
    fun needsInit(): Boolean {
        return manga == null
    }

    /**
     * Initializes this presenter with the given [mangaId] and [initialChapterId]. This method will
     * fetch the manga from the database and initialize the initial chapter.
     */
    suspend fun init(mangaId: Long, initialChapterId: Long): Result<Boolean> {
        return init(mangaId, initialChapterId, initialPageIndex = -1)
    }

    suspend fun init(mangaId: Long, initialChapterId: Long, initialPageIndex: Int): Result<Boolean> {
        if (!needsInit()) return Result.success(true)
        return withIOContext {
            try {
                val entry = getEntry.await(mangaId)
                if (entry != null) {
                    sourceManager.isInitialized.first { it }
                    installViewerSettingBindings(entry)
                    if (chapterId == -1L) chapterId = initialChapterId
                    if (initialPageIndex >= 0) {
                        chapterPageIndex = initialPageIndex
                        initialPageIndexPending = true
                    }

                    val context = Injekt.get<Application>()
                    loader = ChapterLoader(context, downloadManager, downloadProvider, entry, sourceManager)

                    loadChapter(loader!!, chapterList.first { chapterId == it.chapter.id })
                    Result.success(true)
                } else {
                    // Unlikely but okay
                    Result.success(false)
                }
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                Result.failure(e)
            }
        }
    }

    /**
     * Loads the given [chapter] with this [loader] and updates the currently active chapters.
     * Callers must handle errors.
     */
    private suspend fun loadChapter(
        loader: ChapterLoader,
        chapter: ReaderChapter,
    ): ViewerChapters {
        loader.loadChapter(chapter)

        val chapterPos = chapterList.indexOf(chapter)
        val newChapters = ViewerChapters(
            chapter,
            chapterList.getOrNull(chapterPos - 1),
            chapterList.getOrNull(chapterPos + 1),
        )

        withUIContext {
            mutableState.update {
                // Add new references first to avoid unnecessary recycling
                newChapters.ref()
                it.viewerChapters?.unref()

                chapterToDownload = cancelQueuedDownloads(newChapters.current)
                it.copy(
                    viewerChapters = newChapters,
                    bookmarked = newChapters.current.chapter.bookmark,
                )
            }
        }
        return newChapters
    }

    /**
     * Called when the user changed to the given [chapter] when changing pages from the viewer.
     * It's used only to set this chapter as active.
     */
    private fun loadNewChapter(chapter: ReaderChapter) {
        val loader = loader ?: return

        viewModelScope.launchIO {
            logcat { "Loading ${chapter.chapter.url}" }

            updateHistory()
            restartReadTimer()

            try {
                loadChapter(loader, chapter)
            } catch (e: Throwable) {
                if (e is CancellationException) {
                    throw e
                }
                logcat(LogPriority.ERROR, e)
            }
        }
    }

    /**
     * Called when the user is going to load the prev/next chapter through the toolbar buttons.
     */
    private suspend fun loadAdjacent(chapter: ReaderChapter) {
        val loader = loader ?: return

        logcat { "Loading adjacent ${chapter.chapter.url}" }

        mutableState.update { it.copy(isLoadingAdjacentChapter = true) }
        try {
            withIOContext {
                loadChapter(loader, chapter)
            }
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            logcat(LogPriority.ERROR, e)
        } finally {
            mutableState.update { it.copy(isLoadingAdjacentChapter = false) }
        }
    }

    /**
     * Called when the viewers decide it's a good time to preload a [chapter] and improve the UX so
     * that the user doesn't have to wait too long to continue reading.
     */
    suspend fun preload(chapter: ReaderChapter) {
        if (chapter.state is ReaderChapter.State.Loaded || chapter.state == ReaderChapter.State.Loading) {
            return
        }

        if (chapter.pageLoader?.isLocal == false) {
            val chapterManga = chapter.manga ?: manga ?: return
            val dbChapter = chapter.chapter
            val isDownloaded = downloadManager.isChapterDownloaded(
                dbChapter.name,
                dbChapter.scanlator,
                dbChapter.url,
                chapterManga.title,
                chapterManga.source,
                skipCache = true,
            )
            if (isDownloaded) {
                chapter.state = ReaderChapter.State.Wait
            }
        }

        if (chapter.state != ReaderChapter.State.Wait && chapter.state !is ReaderChapter.State.Error) {
            return
        }

        val loader = loader ?: return
        try {
            logcat { "Preloading ${chapter.chapter.url}" }
            loader.loadChapter(chapter)
        } catch (e: Throwable) {
            if (e is CancellationException) {
                throw e
            }
            return
        }
        eventChannel.trySend(Event.ReloadViewerChapters)
    }

    fun onViewerLoaded(viewer: Viewer?) {
        mutableState.update {
            it.copy(viewer = viewer)
        }
    }

    /**
     * Called every time a page changes on the reader. Used to mark the flag of chapters being
     * read, update tracking services, enqueue downloaded chapter deletion, and updating the active chapter if this
     * [page]'s chapter is different from the currently active.
     */
    fun onPageSelected(page: ReaderPage) {
        // InsertPage doesn't change page progress
        if (page is InsertPage) {
            return
        }

        val selectedChapter = page.chapter
        val pages = selectedChapter.pages ?: return

        // Save last page read and mark as read if needed
        viewModelScope.launchNonCancellable {
            updateChapterProgress(selectedChapter, page)
        }

        if (selectedChapter != getCurrentChapter()) {
            logcat { "Setting ${selectedChapter.chapter.url} as active" }
            loadNewChapter(selectedChapter)
        }

        eventChannel.trySend(Event.PageChanged)
    }

    /**
     * Removes [currentChapter] from download queue
     * if setting is enabled and [currentChapter] is queued for download
     */
    private fun cancelQueuedDownloads(currentChapter: ReaderChapter): MangaDownload? {
        return downloadManager.getQueuedDownloadOrNull(currentChapter.chapter.id!!)?.also {
            downloadManager.cancelQueuedDownloads(listOf(it))
        }
    }

    /**
     * Reports the observed chapter progress. Shared Feature participants decide which consequences are allowed.
     */
    private suspend fun updateChapterProgress(readerChapter: ReaderChapter, page: Page) {
        val pageIndex = page.index

        mutableState.update {
            it.copy(currentPage = pageIndex + 1)
        }
        readerChapter.requestedPage = pageIndex
        chapterPageIndex = pageIndex

        if (page.status is Page.State.Error) return
        val entryChapter = readerChapter.chapter.toDomainChapter()?.toEntryChapter() ?: return
        val visibleEntry = manga?.let { getEntry.await(it.id) } ?: return
        val completed = readerChapter.pages?.lastIndex == pageIndex
        if (completed) {
            readerChapter.chapter.read = true
            chapterToDownload = null
        }
        val timestamp = System.currentTimeMillis()
        val pageCount = readerChapter.pages?.size ?: return
        mediaSession.onEvent(
            EntryMediaSessionEvent.Progressed(
                visibleEntry = visibleEntry,
                child = entryChapter,
                progress = mangaProgressState(
                    entryId = entryChapter.entryId,
                    chapterId = entryChapter.id,
                    resourceKey = entryChapter.progressResourceKey,
                    pageIndex = pageIndex.toLong(),
                    pageCount = pageCount.toLong(),
                    completed = readerChapter.chapter.read,
                    locatorUpdatedAt = timestamp,
                    completionUpdatedAt = if (completed) timestamp else 0L,
                ),
                fraction = page.number.toDouble() / pageCount,
                completeEquivalentChildrenByNumber = completed,
                deduplicateDownloadByNumber = readerPreferences.skipDupe.get(),
            ),
        )
    }

    fun restartReadTimer() {
        chapterReadStartTime = Instant.now().toEpochMilli()
    }

    /**
     * Reports completed reader activity through the shared media-session boundary.
     */
    suspend fun updateHistory() {
        getCurrentChapter()?.let { readerChapter ->
            val chapterId = readerChapter.chapter.id!!
            val endTime = Date()
            val sessionReadDuration = chapterReadStartTime?.let { endTime.time - it } ?: 0
            val visibleEntry = manga?.let { getEntry.await(it.id) } ?: return@let
            val child = readerChapter.chapter.toDomainChapter()?.toEntryChapter() ?: return@let
            mediaSession.onEvent(
                EntryMediaSessionEvent.ActivityRecorded(
                    visibleEntry = visibleEntry,
                    child = child,
                    activity = EntryMediaSessionActivity(
                        recordedAtEpochMillis = endTime.time,
                        durationMillis = sessionReadDuration,
                    ),
                ),
            )
            chapterReadStartTime = null
        }
    }

    /**
     * Called from the activity to load and set the next chapter as active.
     */
    suspend fun loadNextChapter() {
        val nextChapter = state.value.viewerChapters?.next ?: return
        loadAdjacent(nextChapter)
    }

    /**
     * Called from the activity to load and set the previous chapter as active.
     */
    suspend fun loadPreviousChapter() {
        val prevChapter = state.value.viewerChapters?.previous ?: return
        loadAdjacent(prevChapter)
    }

    /**
     * Returns the currently active chapter.
     */
    private fun getCurrentChapter(): ReaderChapter? {
        return state.value.currentChapter
    }

    fun resolveChapterWebView(): EntryChildWebViewResolution? {
        val current = getCurrentChapter() ?: return null
        val owner = current.manga ?: manga ?: return null
        val child = current.chapter.toDomainChapter()?.toEntryChapter() ?: return null
        return webViewFeature.resolveChild(owner, child)
    }

    /**
     * Bookmarks the currently active chapter.
     */
    fun toggleChapterBookmark() {
        val current = getCurrentChapter() ?: return
        val owner = current.manga ?: manga ?: return
        val entryChapter = current.chapter.toDomainChapter()?.toEntryChapter() ?: return
        val bookmarked = !entryChapter.bookmark
        current.chapter.bookmark = bookmarked

        viewModelScope.launchNonCancellable {
            bookmarkFeature.setBookmarked(owner, listOf(entryChapter), bookmarked)
        }

        mutableState.update {
            it.copy(
                bookmarked = bookmarked,
            )
        }
    }

    /**
     * Returns the viewer position used by this manga or the default one.
     */
    fun getMangaReadingMode(resolveDefault: Boolean = true): Int {
        val state = state.value
        return if (resolveDefault) state.effectiveReadingMode else state.readingModeOverride
    }

    /**
     * Updates the viewer position for the open manga.
     */
    fun setMangaReadingMode(readingMode: ReadingMode) {
        val manga = manga ?: return
        viewModelScope.launchIO {
            val binding = readingModeBinding ?: return@launchIO
            if (readingMode == ReadingMode.DEFAULT) {
                binding.clearEntryOverride()
            } else {
                binding.setEntryOverride(readingMode.flagValue)
            }
            val resolved = viewerSettingBinder.resolve(readerPreferences.readingModeSetting, manga.id)
            updateReadingMode(resolved)
            val currChapters = state.value.viewerChapters
            if (currChapters != null) {
                // Save current page
                val currChapter = currChapters.current
                currChapter.requestedPage = chapterPageIndex.coerceAtLeast(0)
            }
        }
    }

    /**
     * Returns the orientation type used by this manga or the default one.
     */
    fun getMangaOrientation(resolveDefault: Boolean = true): Int {
        val state = state.value
        return if (resolveDefault) state.effectiveOrientation else state.orientationOverride
    }

    /**
     * Updates the orientation type for the open manga.
     */
    fun setMangaOrientationType(orientation: ReaderOrientation) {
        val manga = manga ?: return
        viewModelScope.launchIO {
            val binding = orientationBinding ?: return@launchIO
            if (orientation == ReaderOrientation.DEFAULT) {
                binding.clearEntryOverride()
            } else {
                binding.setEntryOverride(orientation.flagValue)
            }
            val resolved = viewerSettingBinder.resolve(readerPreferences.orientationSetting, manga.id)
            updateOrientation(resolved)
            val currChapters = state.value.viewerChapters
            if (currChapters != null) {
                // Save current page
                val currChapter = currChapters.current
                currChapter.requestedPage = chapterPageIndex.coerceAtLeast(0)

                mutableState.update {
                    it.copy(
                        viewerChapters = currChapters,
                    )
                }
                eventChannel.send(Event.ReloadViewerChapters)
            }
        }
    }

    private suspend fun installViewerSettingBindings(entry: Entry) {
        val readingMode = viewerSettingBinder.resolve(readerPreferences.readingModeSetting, entry.id)
        val orientation = viewerSettingBinder.resolve(readerPreferences.orientationSetting, entry.id)
        mutableState.update {
            it.copy(
                manga = entry,
                effectiveReadingMode = readingMode.effectiveValue,
                readingModeOverride = readingMode.overrideOrDefault(ReadingMode.DEFAULT.flagValue),
                effectiveOrientation = orientation.effectiveValue,
                orientationOverride = orientation.overrideOrDefault(ReaderOrientation.DEFAULT.flagValue),
            )
        }

        readingModeBinding = viewerSettingBinder.bind(readerPreferences.readingModeSetting, entry.id).also { binding ->
            binding.state
                .drop(1)
                .distinctUntilChanged()
                .onEach(::updateReadingMode)
                .launchIn(viewModelScope)
        }
        orientationBinding = viewerSettingBinder.bind(readerPreferences.orientationSetting, entry.id).also { binding ->
            binding.state
                .drop(1)
                .distinctUntilChanged()
                .onEach(::updateOrientation)
                .launchIn(viewModelScope)
        }
    }

    private fun updateReadingMode(setting: ResolvedViewerSetting<Int>) {
        mutableState.update {
            it.copy(
                effectiveReadingMode = setting.effectiveValue,
                readingModeOverride = setting.overrideOrDefault(ReadingMode.DEFAULT.flagValue),
            )
        }
    }

    private fun updateOrientation(setting: ResolvedViewerSetting<Int>) {
        mutableState.update {
            it.copy(
                effectiveOrientation = setting.effectiveValue,
                orientationOverride = setting.overrideOrDefault(ReaderOrientation.DEFAULT.flagValue),
            )
        }
    }

    private fun ResolvedViewerSetting<Int>.overrideOrDefault(default: Int): Int {
        return if (source == ViewerSettingSource.ENTRY) entryOverride ?: default else default
    }

    fun toggleCropBorders(): Boolean {
        val isPagerType = ReadingMode.isPagerType(getMangaReadingMode())
        return if (isPagerType) {
            readerPreferences.cropBorders.toggle()
        } else {
            readerPreferences.cropBordersWebtoon.toggle()
        }
    }

    /**
     * Generate a filename for the given [manga] and [page]
     */
    private fun generateFilename(
        manga: Entry,
        page: ReaderPage,
    ): String {
        val chapter = page.chapter.chapter
        val filenameSuffix = " - ${page.number}"
        return DiskUtil.buildValidFilename(
            "${manga.displayTitle} - ${chapter.name}",
            DiskUtil.MAX_FILE_NAME_BYTES - filenameSuffix.byteSize(),
        ) + filenameSuffix
    }

    fun showMenus(visible: Boolean) {
        mutableState.update { it.copy(menuVisible = visible) }
    }

    fun showLoadingDialog() {
        mutableState.update { it.copy(dialog = Dialog.Loading) }
    }

    fun openReadingModeSelectDialog() {
        mutableState.update { it.copy(dialog = Dialog.ReadingModeSelect) }
    }

    fun openOrientationModeSelectDialog() {
        mutableState.update { it.copy(dialog = Dialog.OrientationModeSelect) }
    }

    fun openPageDialog(page: ReaderPage) {
        mutableState.update { it.copy(dialog = Dialog.PageActions(page)) }
    }

    fun openSettingsDialog() {
        mutableState.update { it.copy(dialog = Dialog.Settings) }
    }

    fun closeDialog() {
        mutableState.update { it.copy(dialog = null) }
    }

    fun setBrightnessOverlayValue(value: Int) {
        mutableState.update { it.copy(brightnessOverlayValue = value) }
    }

    /**
     * Saves the image of the selected page on the pictures directory and notifies the UI of the result.
     * There's also a notification to allow sharing the image somewhere else or deleting it.
     */
    fun saveImage() {
        val page = (state.value.dialog as? Dialog.PageActions)?.page
        if (page?.status != Page.State.Ready) return
        val manga = manga ?: return

        val context = Injekt.get<Application>()
        val notifier = SaveImageNotifier(context)
        notifier.onClear()

        val filename = generateFilename(manga, page)

        // Pictures directory.
        val relativePath = if (readerPreferences.folderPerManga.get()) {
            DiskUtil.buildValidFilename(
                manga.displayTitle,
            )
        } else {
            ""
        }

        // Copy file in background.
        viewModelScope.launchNonCancellable {
            try {
                val uri = imageSaver.save(
                    image = ReaderImage(
                        inputStream = page.stream!!,
                        name = filename,
                        location = ReaderImageLocation.Pictures.create(relativePath),
                    ),
                )
                withUIContext {
                    notifier.onComplete(uri)
                    eventChannel.send(Event.SavedImage(SaveImageResult.Success(uri)))
                }
            } catch (e: Throwable) {
                notifier.onError(e.message)
                eventChannel.send(Event.SavedImage(SaveImageResult.Error(e)))
            }
        }
    }

    /**
     * Shares the image of the selected page and notifies the UI with the path of the file to share.
     * The image must be first copied to the internal partition because there are many possible
     * formats it can come from, like a zipped chapter, in which case it's not possible to directly
     * get a path to the file and it has to be decompressed somewhere first. Only the last shared
     * image will be kept so it won't be taking lots of internal disk space.
     */
    fun shareImage(copyToClipboard: Boolean) {
        val page = (state.value.dialog as? Dialog.PageActions)?.page
        if (page?.status != Page.State.Ready) return
        val manga = manga ?: return

        val context = Injekt.get<Application>()
        val destDir = File(context.cacheDir, "shared_image")

        val filename = generateFilename(manga, page)

        try {
            viewModelScope.launchNonCancellable {
                destDir.deleteRecursively()
                val uri = imageSaver.save(
                    image = ReaderImage(
                        inputStream = page.stream!!,
                        name = filename,
                        location = ReaderImageLocation.Cache,
                    ),
                )
                eventChannel.send(if (copyToClipboard) Event.CopyImage(uri) else Event.ShareImage(uri, page))
            }
        } catch (e: Throwable) {
            logcat(LogPriority.ERROR, e)
        }
    }

    /**
     * Sets the image of the selected page as cover and notifies the UI of the result.
     */
    fun setAsCover() {
        val page = (state.value.dialog as? Dialog.PageActions)?.page
        if (page?.status != Page.State.Ready) return
        val manga = manga ?: return
        val stream = page.stream ?: return

        viewModelScope.launchNonCancellable {
            val result = try {
                setPageAsCover(manga, stream())
                if (manga.source == LocalSource.ID || manga.favorite) {
                    SetAsCoverResult.Success
                } else {
                    SetAsCoverResult.AddToLibraryFirst
                }
            } catch (e: Exception) {
                SetAsCoverResult.Error
            }
            eventChannel.send(Event.SetCoverResult(result))
        }
    }

    private suspend fun setPageAsCover(manga: Entry, inputStream: InputStream) {
        if (manga.source == LocalSource.ID) {
            localCoverManager.update(manga.url, inputStream)
            updateCoverLastModified(manga.id)
            return
        }

        if (manga.favorite) {
            val context = Injekt.get<Application>()
            val coverDir = context.getExternalFilesDir("covers/custom")
                ?: File(context.filesDir, "covers/custom").also { it.mkdirs() }
            File(coverDir, DiskUtil.hashKeyForDisk(manga.id.toString())).outputStream().use {
                inputStream.copyTo(it)
            }
            updateCoverLastModified(manga.id)
        }
    }

    private suspend fun updateCoverLastModified(entryId: Long): Boolean {
        val entry = entryRepository.getEntryById(entryId) ?: return false
        return entryRepository.update(entry.copy(coverLastModified = Instant.now().toEpochMilli()))
    }

    enum class SetAsCoverResult {
        Success,
        AddToLibraryFirst,
        Error,
    }

    sealed interface SaveImageResult {
        class Success(val uri: Uri) : SaveImageResult
        class Error(val error: Throwable) : SaveImageResult
    }

    /**
     * Deletes all the pending chapters. This operation will run in a background thread and errors
     * are ignored.
     */
    private fun deletePendingChapters() {
        viewModelScope.launchNonCancellable {
            downloadManager.deletePendingChapters()
        }
    }

    /**
     * Loads chapters for [entryId], including merged member entries when present.
     */
    private suspend fun getAllEntryChapters(entry: Entry, applyScanlatorFilter: Boolean): List<EntryChapter> {
        return getEntryWithChapters.awaitChapters(entry, applyScanlatorFilter = applyScanlatorFilter)
    }

    @Immutable
    data class State(
        val manga: Entry? = null,
        val viewerChapters: ViewerChapters? = null,
        val bookmarked: Boolean = false,
        val isLoadingAdjacentChapter: Boolean = false,
        val currentPage: Int = -1,
        val effectiveReadingMode: Int = ReadingMode.RIGHT_TO_LEFT.flagValue,
        val readingModeOverride: Int = ReadingMode.DEFAULT.flagValue,
        val effectiveOrientation: Int = ReaderOrientation.FREE.flagValue,
        val orientationOverride: Int = ReaderOrientation.DEFAULT.flagValue,

        /**
         * Viewer used to display the pages (pager, webtoon, ...).
         */
        val viewer: Viewer? = null,
        val dialog: Dialog? = null,
        val menuVisible: Boolean = false,
        @IntRange(from = -100, to = 100) val brightnessOverlayValue: Int = 0,
    ) {
        val currentChapter: ReaderChapter?
            get() = viewerChapters?.current

        val totalPages: Int
            get() = currentChapter?.pages?.size ?: -1
    }

    sealed interface Dialog {
        data object Loading : Dialog
        data object Settings : Dialog
        data object ReadingModeSelect : Dialog
        data object OrientationModeSelect : Dialog
        data class PageActions(val page: ReaderPage) : Dialog
    }

    sealed interface Event {
        data object ReloadViewerChapters : Event
        data object PageChanged : Event
        data class SetCoverResult(val result: SetAsCoverResult) : Event

        data class SavedImage(val result: SaveImageResult) : Event
        data class ShareImage(val uri: Uri, val page: ReaderPage) : Event
        data class CopyImage(val uri: Uri) : Event
    }
}
