package eu.kanade.tachiyomi.ui.entry.preview

import android.content.Context
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import eu.kanade.presentation.entry.entryTypePresentation
import eu.kanade.tachiyomi.source.entry.EntryType
import eu.kanade.tachiyomi.ui.entry.EntryStateAccess
import eu.kanade.tachiyomi.ui.entry.chapters.EntryChapterList
import eu.kanade.tachiyomi.ui.entry.chapters.applyFilters
import eu.kanade.tachiyomi.ui.entry.members.EntryMembers
import eu.kanade.tachiyomi.ui.entry.refresh.EntrySourceRefresh
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import mihon.entry.interactions.media.EntryPreviewAvailability
import mihon.entry.interactions.media.EntryPreviewChildCandidate
import mihon.entry.interactions.media.EntryPreviewConfig
import mihon.entry.interactions.media.EntryPreviewContext
import mihon.entry.interactions.media.EntryPreviewFeature
import mihon.entry.interactions.media.EntryPreviewHandle
import mihon.entry.interactions.media.EntryPreviewLoadRequest
import mihon.entry.interactions.media.EntryPreviewLoadResult
import mihon.entry.interactions.media.EntryPreviewOpenTargetResult
import mihon.entry.interactions.media.EntryPreviewPageStatus
import mihon.entry.interactions.presentation.EntryTypePresentationFeature
import tachiyomi.core.common.util.lang.launchIO
import tachiyomi.domain.entry.interactor.GetEntry
import tachiyomi.domain.entry.model.Entry
import tachiyomi.domain.library.service.LibraryPreferences
import tachiyomi.domain.source.service.SourceManager
import tachiyomi.i18n.context.stringResource

/**
 * The page preview an entry screen shows under its description: whether the entry offers one and at what size, and the
 * pages of the chapter it previews, loaded when the preview opens and released when it closes.
 *
 * @param chapterItems the chapters the screen covers as list items, which the preview picks its chapter from.
 */
@AssistedInject
class EntryPreviewModel(
    @Assisted private val scope: CoroutineScope,
    @Assisted private val gatedScope: CoroutineScope,
    @Assisted private val context: Context,
    @Assisted private val entryId: Long,
    @Assisted private val state: EntryStateAccess,
    @Assisted private val members: EntryMembers,
    @Assisted private val sourceRefresh: EntrySourceRefresh,
    @Assisted private val chapterItems: suspend (Entry) -> List<EntryChapterList.Item>,
    private val entryPreviewFeature: EntryPreviewFeature,
    private val entryTypePresentationFeature: EntryTypePresentationFeature,
    private val getEntry: GetEntry,
    private val libraryPreferences: LibraryPreferences,
    private val sourceManager: SourceManager,
) {

    @AssistedFactory
    fun interface Factory {
        fun create(
            scope: CoroutineScope,
            gatedScope: CoroutineScope,
            context: Context,
            entryId: Long,
            state: EntryStateAccess,
            members: EntryMembers,
            sourceRefresh: EntrySourceRefresh,
            chapterItems: suspend (Entry) -> List<EntryChapterList.Item>,
        ): EntryPreviewModel
    }

    private val configState = MutableStateFlow(EntryPreviewConfig.Disabled)
    val config: StateFlow<EntryPreviewConfig> = configState.asStateFlow()

    private val loaderState = MutableStateFlow(EntryPreviewState(pageCount = configState.value.pageCount))
    val previewState: StateFlow<EntryPreviewState> = loaderState.asStateFlow()

    private var handle: EntryPreviewHandle? = null
    private var loadJob: Job? = null
    private var pageJobs: Map<Int, Job> = emptyMap()
    private var configJob: Job? = null
    private var configEntryKey: Pair<Long, EntryType>? = null

    fun isEnabled(entry: Entry): Boolean {
        return entryPreviewFeature.availability(previewContext(entry)) is EntryPreviewAvailability.Available
    }

    fun isOpenApplicable(type: EntryType): Boolean = entryPreviewFeature.isOpenApplicable(type)

    /** Follows whether [entry] offers a preview, from the first time the screen shows it or another entry type. */
    fun observeConfig(entry: Entry) {
        val key = entry.id to entry.type
        if (configEntryKey == key) return

        configEntryKey = key
        configJob?.cancel()
        applyAvailability(
            availability = entryPreviewFeature.availability(previewContext(entry)),
            reloadExpanded = false,
        )
        configJob = gatedScope.launchIO {
            entryPreviewFeature.availabilityChanges(previewContext(entry))
                .collectLatest { availability ->
                    applyAvailability(
                        availability = availability,
                        reloadExpanded = true,
                    )
                }
        }
    }

    fun setExpanded(expanded: Boolean) {
        if (!isAvailable()) {
            collapse()
            return
        }

        if (expanded) {
            loaderState.update { it.copy(isExpanded = true) }
            if (!previewState.value.hasLoadedContent) {
                load()
            }
        } else {
            collapse()
        }
    }

    fun retry() {
        if (!previewState.value.isExpanded) return
        load(force = true)
    }

    fun loadPage(pageIndex: Int) {
        val handle = handle ?: return
        val page = handle.pages.firstOrNull { it.index == pageIndex } ?: return
        if (page.status.value == EntryPreviewPageStatus.Ready) return
        if (pageJobs[pageIndex]?.isActive == true) return

        pageJobs = pageJobs + (
            pageIndex to scope.launchIO {
                try {
                    entryPreviewFeature.loadPage(handle, pageIndex)
                } catch (_: Throwable) {
                    // Page state carries the failure.
                }
            }
            )
    }

    fun openTarget(pageIndex: Int): EntryPreviewOpenTargetResult {
        val handle = handle ?: return EntryPreviewOpenTargetResult.NotOpenable
        return entryPreviewFeature.openTarget(handle, pageIndex)
    }

    fun release() {
        configJob?.cancel()
        clearResources(resetState = false)
    }

    private fun isAvailable(entry: Entry? = state.loaded?.entry): Boolean {
        return entry != null && isEnabled(entry)
    }

    private fun previewContext(entry: Entry): EntryPreviewContext = EntryPreviewContext(
        entry = entry,
        source = sourceManager.getOrStub(entry.source),
    )

    private fun applyAvailability(
        availability: EntryPreviewAvailability,
        reloadExpanded: Boolean,
    ) {
        applyConfig(
            config = availability.config.copy(enabled = availability is EntryPreviewAvailability.Available),
            reloadExpanded = reloadExpanded,
        )
    }

    private fun applyConfig(
        config: EntryPreviewConfig,
        reloadExpanded: Boolean,
    ) {
        val previousConfig = configState.value
        configState.value = config

        loaderState.update { preview ->
            val updatedPages = if (preview.isExpanded && preview.pages.isNotEmpty()) {
                preview.pages.take(config.pageCount)
            } else {
                preview.pages
            }
            preview.copy(pageCount = config.pageCount, pages = updatedPages)
        }

        if (!config.enabled) {
            collapse()
            return
        }

        if (reloadExpanded && previewState.value.isExpanded && previousConfig.pageCount != config.pageCount) {
            load(force = true)
        }
    }

    private fun load(force: Boolean = false) {
        if (!isAvailable()) return
        val pageCount = configState.value.pageCount

        loadJob?.cancel()
        loadJob = scope.launchIO {
            clearResources(resetState = false, cancelLoadJob = false)
            loaderState.update {
                it.copy(
                    isExpanded = true,
                    isLoading = true,
                    error = null,
                    chapterId = null,
                    pages = emptyList(),
                    pageCount = pageCount,
                    hasLoaded = false,
                )
            }

            sourceRefresh.await()
            val loaded = state.loaded ?: return@launchIO
            if (!isEnabled(loaded.entry)) {
                collapse()
                return@launchIO
            }

            if (!force && previewState.value.hasLoadedContent) {
                loaderState.update { it.copy(isLoading = false) }
                return@launchIO
            }

            runCatching {
                val latestEntry = requireNotNull(getEntry.await(entryId)) {
                    "Preview entry $entryId no longer exists"
                }
                when (
                    val result = entryPreviewFeature.load(
                        EntryPreviewLoadRequest(
                            context = context,
                            previewContext = previewContext(latestEntry),
                            children = candidates(latestEntry),
                            memberIds = members.presentation(latestEntry).memberIds,
                        ),
                    )
                ) {
                    is EntryPreviewLoadResult.Loaded -> result.handle
                    is EntryPreviewLoadResult.Disabled -> error("Preview was disabled before loading")
                    is EntryPreviewLoadResult.Inapplicable -> error("Preview is not supported for ${result.type}")
                    is EntryPreviewLoadResult.ContextuallyUnavailable -> error(
                        context.stringResource(
                            latestEntry.type.entryTypePresentation(entryTypePresentationFeature).noChildrenFoundLabel,
                        ),
                    )
                }
            }.onSuccess { handle ->
                this@EntryPreviewModel.handle = handle
                val loadedPages = handle.pages.map(::PreviewPage)

                loaderState.update {
                    it.copy(
                        isExpanded = true,
                        isLoading = false,
                        error = null,
                        chapterId = handle.chapterId,
                        pages = loadedPages,
                        pageCount = pageCount,
                        hasLoaded = true,
                    )
                }
            }.onFailure { error ->
                if (error is CancellationException) {
                    return@onFailure
                }
                clearResources(resetState = false, cancelLoadJob = false)
                loaderState.update {
                    it.copy(
                        isExpanded = true,
                        isLoading = false,
                        error = error,
                        chapterId = null,
                        pages = emptyList(),
                        pageCount = pageCount,
                        hasLoaded = false,
                    )
                }
            }
        }
    }

    private suspend fun candidates(entry: Entry): List<EntryPreviewChildCandidate> {
        return chapterItems(entry).applyFilters(entry, libraryPreferences.downloadedOnly.get()).map { item ->
            EntryPreviewChildCandidate(
                entry = item.entry,
                child = item.chapter,
                source = sourceManager.getOrStub(item.entry.source),
            )
        }
    }

    private fun collapse() {
        clearResources(resetState = true)
        loaderState.update {
            EntryPreviewState(
                isExpanded = false,
                pageCount = configState.value.pageCount,
            )
        }
    }

    private fun clearResources(resetState: Boolean, cancelLoadJob: Boolean = true) {
        if (cancelLoadJob) {
            loadJob?.cancel()
            loadJob = null
        }
        pageJobs.values.forEach(Job::cancel)
        pageJobs = emptyMap()
        handle?.let(entryPreviewFeature::release)
        handle = null
        if (resetState) {
            loaderState.value = EntryPreviewState(pageCount = configState.value.pageCount)
        }
    }
}
